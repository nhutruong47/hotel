-- ============================================================
-- V3__add_query_path_indexes.sql
-- Composite indexes matched to repository query paths.
-- Kept ANSI/H2-compatible; PostgreSQL-specific indexes live separately.
-- ============================================================

-- Booking list, scheduler, availability, and reporting paths.
CREATE INDEX IF NOT EXISTS idx_bookings_status_created
    ON bookings(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bookings_status_payment_deadline
    ON bookings(status, payment_deadline);

CREATE INDEX IF NOT EXISTS idx_bookings_status_hold_expires
    ON bookings(status, hold_expires_at);

CREATE INDEX IF NOT EXISTS idx_bookings_room_status_checkout_checkin
    ON bookings(room_id, status, check_out_date, check_in_date);

CREATE INDEX IF NOT EXISTS idx_bookings_refund_modified
    ON bookings(refund_amount, modified_at DESC);

-- Payment paths by booking/user/status and recent history.
CREATE INDEX IF NOT EXISTS idx_payments_booking_created
    ON payments(booking_id, created_at DESC);

-- Review paths by user/newest, admin newest, and duplicate checks.
CREATE INDEX IF NOT EXISTS idx_reviews_user_created
    ON reviews(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reviews_created
    ON reviews(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_reviews_user_room
    ON reviews(user_id, room_id);

-- Content/admin listing paths.
CREATE INDEX IF NOT EXISTS idx_promotions_active_display
    ON promotions(is_active, display_order ASC);

CREATE INDEX IF NOT EXISTS idx_promotions_category_active_display
    ON promotions(category, is_active, display_order ASC);

CREATE INDEX IF NOT EXISTS idx_promotions_featured_window_display
    ON promotions(is_active, is_featured, start_date, end_date, display_order ASC);

CREATE INDEX IF NOT EXISTS idx_promotions_active_end
    ON promotions(is_active, end_date DESC);

CREATE INDEX IF NOT EXISTS idx_promotions_active_start
    ON promotions(is_active, start_date ASC);

CREATE INDEX IF NOT EXISTS idx_promotions_active_countdown
    ON promotions(is_active, countdown_end_date);

CREATE INDEX IF NOT EXISTS idx_faq_published_display
    ON faq_items(is_published, display_order ASC);

CREATE INDEX IF NOT EXISTS idx_faq_category_published_display
    ON faq_items(category, is_published, display_order ASC);

CREATE INDEX IF NOT EXISTS idx_contact_messages_type_created
    ON contact_messages(type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_contact_messages_status_type
    ON contact_messages(status, type);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action_created
    ON audit_logs(action, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created
    ON audit_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_user_created
    ON chat_messages(user_id, created_at DESC);
