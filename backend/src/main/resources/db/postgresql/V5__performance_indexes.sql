-- ============================================================
-- V5__performance_indexes.sql
-- Composite indexes based on frequent query patterns
-- ============================================================

-- Bookings table
-- Optimization for findExpiredPaymentBookings
CREATE INDEX IF NOT EXISTS idx_bookings_status_payment_dl 
    ON bookings(status, payment_deadline);

-- Optimization for findExpiredHoldBookings
CREATE INDEX IF NOT EXISTS idx_bookings_status_hold_expires 
    ON bookings(status, hold_expires_at);

-- Optimization for findSuccessfulBookingsInDateRange
CREATE INDEX IF NOT EXISTS idx_bookings_dates_status 
    ON bookings(check_in_date, check_out_date, status);

-- Promotions table
-- Optimization for findActivePromotions (in PromotionRepository)
CREATE INDEX IF NOT EXISTS idx_promotions_active_dates 
    ON promotions(is_active, start_date, end_date);

-- Payments table
-- Optimization for finding payments by status and booking
CREATE INDEX IF NOT EXISTS idx_payments_booking_status 
    ON payments(booking_id, status);

-- Reviews table
-- Optimization for find reviews by room
CREATE INDEX IF NOT EXISTS idx_reviews_room_rating 
    ON reviews(room_id, rating);
