-- =============================================================================
-- V17 — Domain CHECK constraints and natural uniqueness
-- -----------------------------------------------------------------------------
-- Enum columns are stored as VARCHAR (@Enumerated(STRING)); without CHECKs any
-- string can be written by SQL tooling or a future bug. Money must never be
-- negative; ratings are 1..5; a booking has at most one review; promo codes are
-- unique (case-insensitive, as they are matched that way).
-- Value lists mirror the Java enums (Payment, ContactMessage, VillaMaintenance,
-- VillaInspection, BookingStatus, Promotion.PromotionCategory). Adding an enum
-- value now REQUIRES a migration — see the review checklist in database-design.md.
-- Pre-flight aborts with a per-rule count; no business data is modified.
-- Rollback: db/rollback/U17__domain_checks.sql
-- =============================================================================

DO $$
DECLARE
    problems TEXT := '';
    n BIGINT;
BEGIN
    SELECT count(*) INTO n FROM reviews WHERE rating NOT BETWEEN 1 AND 5;
    IF n > 0 THEN problems := problems || format('reviews.rating outside 1..5: %s; ', n); END IF;

    SELECT count(*) INTO n FROM reviews
     WHERE (rating_cleanliness IS NOT NULL AND rating_cleanliness NOT BETWEEN 1 AND 5)
        OR (rating_service     IS NOT NULL AND rating_service     NOT BETWEEN 1 AND 5)
        OR (rating_location    IS NOT NULL AND rating_location    NOT BETWEEN 1 AND 5)
        OR (rating_value       IS NOT NULL AND rating_value       NOT BETWEEN 1 AND 5)
        OR (rating_amenities   IS NOT NULL AND rating_amenities   NOT BETWEEN 1 AND 5);
    IF n > 0 THEN problems := problems || format('reviews sub-ratings outside 1..5: %s; ', n); END IF;

    SELECT count(*) INTO n FROM (SELECT booking_id FROM reviews WHERE booking_id IS NOT NULL
                                  GROUP BY booking_id HAVING count(*) > 1) d;
    IF n > 0 THEN problems := problems || format('bookings with more than one review: %s; ', n); END IF;

    SELECT count(*) INTO n FROM bookings
     WHERE total_price < 0 OR subtotal_price < 0 OR service_fee < 0 OR tax_amount < 0
        OR discount_amount < 0 OR refund_amount < 0
        OR (refund_percentage IS NOT NULL AND refund_percentage NOT BETWEEN 0 AND 100)
        OR (guests IS NOT NULL AND guests <= 0);
    IF n > 0 THEN problems := problems || format('bookings with negative money/guests or bad refund %%: %s; ', n); END IF;

    SELECT count(*) INTO n FROM payments
     WHERE amount < 0 OR refund_amount < 0 OR (refund_amount IS NOT NULL AND refund_amount > amount)
        OR status NOT IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED')
        OR method NOT IN ('BANK_TRANSFER', 'QR_CODE', 'CASH', 'CARD', 'OTHER')
        OR (gateway IS NOT NULL AND gateway NOT IN ('STRIPE', 'VNPAY', 'VIETQR', 'SEPAY', 'MOCK'));
    IF n > 0 THEN problems := problems || format('payments with invalid amount/status/method/gateway: %s; ', n); END IF;

    SELECT count(*) INTO n FROM vouchers WHERE amount < 0 OR quantity < 0
        OR (is_percent AND amount > 100);
    IF n > 0 THEN problems := problems || format('vouchers with invalid amount/quantity: %s; ', n); END IF;

    SELECT count(*) INTO n FROM promotions
     WHERE start_date > end_date OR current_uses < 0 OR (maximum_uses IS NOT NULL AND maximum_uses < 0)
        OR minimum_booking_amount < 0 OR minimum_nights < 0
        OR (discount_percent IS NOT NULL AND discount_percent NOT BETWEEN 0 AND 100)
        OR (discount_amount IS NOT NULL AND discount_amount < 0);
    IF n > 0 THEN problems := problems || format('promotions with invalid dates/uses/discounts: %s; ', n); END IF;

    SELECT count(*) INTO n FROM (SELECT UPPER(promo_code) FROM promotions WHERE promo_code IS NOT NULL
                                  GROUP BY UPPER(promo_code) HAVING count(*) > 1) d;
    IF n > 0 THEN problems := problems || format('duplicate promo codes (case-insensitive): %s; ', n); END IF;

    SELECT count(*) INTO n FROM rooms
     WHERE price_per_night < 0 OR minimum_stay < 1 OR maximum_stay < minimum_stay
        OR (capacity IS NOT NULL AND capacity <= 0);
    IF n > 0 THEN problems := problems || format('rooms with invalid price/stay/capacity: %s; ', n); END IF;

    SELECT count(*) INTO n FROM booking_status_transitions
     WHERE (from_status IS NOT NULL AND from_status NOT IN
              ('PENDING_PAYMENT','PAID','CHECKED_IN','CHECKED_OUT','COMPLETED','CANCELLED','EXPIRED','NO_SHOW'))
        OR (to_status IS NOT NULL AND to_status NOT IN
              ('PENDING_PAYMENT','PAID','CHECKED_IN','CHECKED_OUT','COMPLETED','CANCELLED','EXPIRED','NO_SHOW'));
    IF n > 0 THEN problems := problems || format('booking_status_transitions with unknown status: %s; ', n); END IF;

    SELECT count(*) INTO n FROM contact_messages
     WHERE status NOT IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')
        OR priority NOT IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')
        OR type NOT IN ('GENERAL', 'RESERVATION', 'SUPPORT', 'FEEDBACK', 'PARTNERSHIP', 'EMERGENCY');
    IF n > 0 THEN problems := problems || format('contact_messages with unknown status/priority/type: %s; ', n); END IF;

    SELECT count(*) INTO n FROM villa_maintenances
     WHERE status NOT IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
    IF n > 0 THEN problems := problems || format('villa_maintenances with unknown status: %s; ', n); END IF;

    SELECT count(*) INTO n FROM villa_inspections
     WHERE status NOT IN ('SCHEDULED', 'IN_PROGRESS', 'PASSED', 'FLAGGED_ISSUES', 'REPAIRED', 'CANCELLED')
        OR severity_level NOT IN ('NORMAL', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
        OR inspection_type NOT IN ('ROUTINE_CHECK', 'PRE_CHECKIN', 'POST_CHECKOUT', 'DRONE_ROOF_SURVEY',
                                   'DRONE_FACADE_SURVEY', 'THERMAL_FACADE', 'POOL_FACILITY', 'MAINTENANCE_AUDIT');
    IF n > 0 THEN problems := problems || format('villa_inspections with unknown status/severity/type: %s; ', n); END IF;

    IF problems <> '' THEN
        RAISE EXCEPTION 'V17 aborted: %', problems
            USING HINT = 'Query each table with the predicate named above, fix the rows deliberately, then re-run.';
    END IF;
END $$;

-- ----- reviews -----
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5);
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_sub_ratings CHECK (
        (rating_cleanliness IS NULL OR rating_cleanliness BETWEEN 1 AND 5)
    AND (rating_service     IS NULL OR rating_service     BETWEEN 1 AND 5)
    AND (rating_location    IS NULL OR rating_location    BETWEEN 1 AND 5)
    AND (rating_value       IS NULL OR rating_value       BETWEEN 1 AND 5)
    AND (rating_amenities   IS NULL OR rating_amenities   BETWEEN 1 AND 5));
CREATE UNIQUE INDEX IF NOT EXISTS uq_reviews_booking ON reviews (booking_id) WHERE booking_id IS NOT NULL;

-- ----- bookings -----
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_money_non_negative CHECK (
        total_price >= 0
    AND (subtotal_price  IS NULL OR subtotal_price  >= 0)
    AND (service_fee     IS NULL OR service_fee     >= 0)
    AND (tax_amount      IS NULL OR tax_amount      >= 0)
    AND (discount_amount IS NULL OR discount_amount >= 0)
    AND (refund_amount   IS NULL OR refund_amount   >= 0));
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_refund_percentage
    CHECK (refund_percentage IS NULL OR refund_percentage BETWEEN 0 AND 100);
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_guests CHECK (guests IS NULL OR guests > 0);

-- ----- payments -----
ALTER TABLE payments ADD CONSTRAINT chk_payments_amounts CHECK (
        amount >= 0
    AND (refund_amount IS NULL OR (refund_amount >= 0 AND refund_amount <= amount)));
ALTER TABLE payments ADD CONSTRAINT chk_payments_status
    CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_method
    CHECK (method IN ('BANK_TRANSFER', 'QR_CODE', 'CASH', 'CARD', 'OTHER'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_gateway
    CHECK (gateway IS NULL OR gateway IN ('STRIPE', 'VNPAY', 'VIETQR', 'SEPAY', 'MOCK'));

-- ----- vouchers / promotions -----
ALTER TABLE vouchers ADD CONSTRAINT chk_vouchers_values
    CHECK (amount >= 0 AND quantity >= 0 AND (NOT is_percent OR amount <= 100));
ALTER TABLE promotions ADD CONSTRAINT chk_promotions_values CHECK (
        start_date <= end_date
    AND current_uses >= 0
    AND (maximum_uses IS NULL OR maximum_uses >= 0)
    AND minimum_booking_amount >= 0
    AND minimum_nights >= 0
    AND (discount_percent IS NULL OR discount_percent BETWEEN 0 AND 100)
    AND (discount_amount  IS NULL OR discount_amount >= 0));
CREATE UNIQUE INDEX IF NOT EXISTS uq_promotions_promo_code ON promotions (UPPER(promo_code)) WHERE promo_code IS NOT NULL;

-- ----- rooms -----
ALTER TABLE rooms ADD CONSTRAINT chk_rooms_values CHECK (
        price_per_night >= 0
    AND minimum_stay >= 1
    AND maximum_stay >= minimum_stay
    AND (capacity IS NULL OR capacity > 0));

-- ----- status enums -----
ALTER TABLE booking_status_transitions ADD CONSTRAINT chk_bst_statuses CHECK (
        (from_status IS NULL OR from_status IN
            ('PENDING_PAYMENT','PAID','CHECKED_IN','CHECKED_OUT','COMPLETED','CANCELLED','EXPIRED','NO_SHOW'))
    AND (to_status IS NULL OR to_status IN
            ('PENDING_PAYMENT','PAID','CHECKED_IN','CHECKED_OUT','COMPLETED','CANCELLED','EXPIRED','NO_SHOW')));
ALTER TABLE contact_messages ADD CONSTRAINT chk_contact_enums CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')
    AND priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')
    AND type IN ('GENERAL', 'RESERVATION', 'SUPPORT', 'FEEDBACK', 'PARTNERSHIP', 'EMERGENCY'));
ALTER TABLE villa_maintenances ADD CONSTRAINT chk_maint_status
    CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));
ALTER TABLE villa_inspections ADD CONSTRAINT chk_inspection_enums CHECK (
        status IN ('SCHEDULED', 'IN_PROGRESS', 'PASSED', 'FLAGGED_ISSUES', 'REPAIRED', 'CANCELLED')
    AND severity_level IN ('NORMAL', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    AND inspection_type IN ('ROUTINE_CHECK', 'PRE_CHECKIN', 'POST_CHECKOUT', 'DRONE_ROOF_SURVEY',
                            'DRONE_FACADE_SURVEY', 'THERMAL_FACADE', 'POOL_FACILITY', 'MAINTENANCE_AUDIT'));
