-- ============================================================
-- V2__add_performance_indexes.sql
-- Performance indexes for common query patterns
-- ============================================================

-- Booking indexes for status and date filtering
CREATE INDEX IF NOT EXISTS idx_bookings_status_dates
    ON bookings(status, check_in_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_bookings_user_created
    ON bookings(user_id, created_at DESC);

-- Payment indexes for gateway operations
CREATE INDEX IF NOT EXISTS idx_payments_intent ON payments(intent_id);
CREATE INDEX IF NOT EXISTS idx_payments_status_created ON payments(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_booking_status ON payments(booking_id, status);

-- User indexes for login and lookup
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role_email ON users(role, email);

-- Audit log indexes for admin queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created ON audit_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- Review indexes
CREATE INDEX IF NOT EXISTS idx_reviews_room_hidden_created ON reviews(room_id, is_hidden, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_booking_user ON reviews(booking_id, user_id);

-- Notification indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created
    ON notifications(user_id, is_read, created_at DESC);

-- Promotion indexes
CREATE INDEX IF NOT EXISTS idx_promotions_active_window
    ON promotions(is_active, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(promo_code);

-- Wishlist indexes
CREATE INDEX IF NOT EXISTS idx_wishlists_room ON wishlists(room_id);

-- Contact message indexes
CREATE INDEX IF NOT EXISTS idx_contact_messages_status_created
    ON contact_messages(status, created_at DESC);
