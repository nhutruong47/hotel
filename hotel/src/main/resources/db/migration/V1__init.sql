# ============================================================
# Flyway baseline migration — V1
#
# This migration establishes the schema that matches the current
# @Entity definitions. Hibernate `ddl-auto=validate` will refuse to boot if
# the database schema drifts from this baseline, surfacing drift early
# rather than allowing silent mutation.
#
# Idempotent: every CREATE statement uses IF NOT EXISTS so re-running this
# script against an already-initialised H2 dev database is harmless.
# ============================================================

-- ----- Users -----
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    email         VARCHAR(255),
    role          VARCHAR(32)  NOT NULL DEFAULT 'USER',
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_token  VARCHAR(255),
    token_expiry       TIMESTAMP NULL,
    reset_token        VARCHAR(255),
    reset_token_expiry TIMESTAMP NULL,
    avatar_filename    VARCHAR(255),
    phone              VARCHAR(64),
    date_of_birth      VARCHAR(32),
    gender             VARCHAR(32),
    nationality        VARCHAR(96),
    emergency_contact_name     VARCHAR(255),
    emergency_contact_phone    VARCHAR(64),
    emergency_contact_relation VARCHAR(64),
    identity_type       VARCHAR(64),
    identity_number     VARCHAR(128),
    address             VARCHAR(255),
    city                VARCHAR(96),
    country             VARCHAR(96),
    postal_code         VARCHAR(32),
    delete_requested        BOOLEAN      NOT NULL DEFAULT FALSE,
    delete_requested_at     TIMESTAMP    NULL,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disabled               BOOLEAN      NOT NULL DEFAULT FALSE,
    disabled_at            TIMESTAMP    NULL,
    disabled_reason        VARCHAR(255),
    preferences_json       TEXT
);

CREATE INDEX IF NOT EXISTS idx_users_username      ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email         ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_reset_token   ON users(reset_token);
CREATE INDEX IF NOT EXISTS idx_users_verif_token   ON users(verification_token);
CREATE INDEX IF NOT EXISTS idx_users_role          ON users(role);

-- ----- Room types -----
CREATE TABLE IF NOT EXISTS room_types (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    description TEXT
);

-- ----- Amenities -----
CREATE TABLE IF NOT EXISTS amenities (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(128) NOT NULL UNIQUE,
    icon_code  VARCHAR(64)
);

-- ----- Rooms -----
CREATE TABLE IF NOT EXISTS rooms (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    room_number        VARCHAR(64) NOT NULL UNIQUE,
    room_type_id       INT         NOT NULL,
    price_per_night    DECIMAL(12, 0) NOT NULL,
    description        TEXT,
    image_url          VARCHAR(512),
    is_available       BOOLEAN     NOT NULL DEFAULT TRUE,
    capacity           INT         NOT NULL DEFAULT 2,
    bedrooms           INT         NOT NULL DEFAULT 1,
    avg_rating         DECIMAL(3, 2) DEFAULT 0,
    review_count       BIGINT      NOT NULL DEFAULT 0,
    house_rules        TEXT,
    policies           TEXT,
    nearby_attractions TEXT,
    check_in_time      VARCHAR(16) DEFAULT '14:00',
    check_out_time     VARCHAR(16) DEFAULT '12:00',
    latitude           DOUBLE,
    longitude          DOUBLE,
    nearby_restaurants TEXT,
    nearby_cafes       TEXT,
    nearby_airport     TEXT,
    directions         TEXT,
    minimum_stay       INT         NOT NULL DEFAULT 1,
    maximum_stay       INT         NOT NULL DEFAULT 30,
    CONSTRAINT fk_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(id)
);
CREATE INDEX IF NOT EXISTS idx_rooms_available  ON rooms(is_available);
CREATE INDEX IF NOT EXISTS idx_rooms_room_type  ON rooms(room_type_id);
CREATE INDEX IF NOT EXISTS idx_rooms_room_number ON rooms(room_number);

CREATE TABLE IF NOT EXISTS room_gallery_images (
    room_id    INT  NOT NULL,
    image_url  VARCHAR(512),
    CONSTRAINT fk_room_gallery FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE TABLE IF NOT EXISTS room_amenities (
    room_id     INT NOT NULL,
    amenity_id  INT NOT NULL,
    PRIMARY KEY (room_id, amenity_id),
    CONSTRAINT fk_room_amenities_room    FOREIGN KEY (room_id)    REFERENCES rooms(id),
    CONSTRAINT fk_room_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(id)
);

-- ----- Vouchers -----
CREATE TABLE IF NOT EXISTS vouchers (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    amount      DECIMAL(12, 0) NOT NULL,
    expiry_date DATE,
    quantity    INT          NOT NULL DEFAULT 1,
    is_percent  BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_vouchers_code   ON vouchers(code);
CREATE INDEX IF NOT EXISTS idx_vouchers_expiry ON vouchers(expiry_date);

-- ----- Promotions -----
CREATE TABLE IF NOT EXISTS promotions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    subtitle        VARCHAR(255),
    description     TEXT NOT NULL,
    terms_conditions TEXT,
    image_url       VARCHAR(512),
    banner_url      VARCHAR(512),
    category        VARCHAR(64)  NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_featured     BOOLEAN      NOT NULL DEFAULT FALSE,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    countdown_end_date TIMESTAMP NULL,
    discount_percent    DECIMAL(5, 2),
    discount_amount     DECIMAL(12, 0),
    minimum_booking_amount DECIMAL(12, 0) NOT NULL DEFAULT 0,
    minimum_nights      INT NOT NULL DEFAULT 1,
    maximum_uses        INT,
    current_uses        INT NOT NULL DEFAULT 0,
    promo_code          VARCHAR(64),
    display_order       INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_promotion_active   ON promotions(is_active);
CREATE INDEX IF NOT EXISTS idx_promotion_start    ON promotions(start_date);
CREATE INDEX IF NOT EXISTS idx_promotion_end      ON promotions(end_date);
CREATE INDEX IF NOT EXISTS idx_promotion_category ON promotions(category);

CREATE TABLE IF NOT EXISTS promotion_rooms (
    promotion_id INT NOT NULL,
    room_id      INT NOT NULL,
    PRIMARY KEY (promotion_id, room_id),
    CONSTRAINT fk_promo_rooms_promo FOREIGN KEY (promotion_id) REFERENCES promotions(id),
    CONSTRAINT fk_promo_rooms_room  FOREIGN KEY (room_id)      REFERENCES rooms(id)
);

-- ----- Bookings -----
CREATE TABLE IF NOT EXISTS bookings (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    version                BIGINT      NOT NULL DEFAULT 0,
    user_id                INT         NOT NULL,
    room_id                INT         NOT NULL,
    check_in_date          DATE        NOT NULL,
    check_out_date         DATE        NOT NULL,
    status                 VARCHAR(32) NOT NULL,
    total_price            DECIMAL(12, 0) NOT NULL,
    subtotal_price         DECIMAL(12, 0),
    service_fee            DECIMAL(12, 0) DEFAULT 0,
    tax_amount             DECIMAL(12, 0) DEFAULT 0,
    refund_amount          DECIMAL(12, 0),
    refund_percentage      INT,
    guest_name             VARCHAR(255),
    guest_phone            VARCHAR(64),
    guest_email            VARCHAR(255),
    guests                 INT,
    notes                  TEXT,
    cancellation_reason    TEXT,
    applied_voucher_code   VARCHAR(64),
    discount_amount        DECIMAL(12, 0) DEFAULT 0,
    approved_by            INT,
    approved_at            TIMESTAMP NULL,
    rejection_reason       TEXT,
    payment_deadline       TIMESTAMP NOT NULL,
    paid_at                TIMESTAMP NULL,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at            TIMESTAMP NULL,
    checked_in_at          TIMESTAMP NULL,
    checked_out_at         TIMESTAMP NULL,
    hold_expires_at        TIMESTAMP NULL,
    cancelled_at           TIMESTAMP NULL,
    cancelled_by           VARCHAR(64),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms(id)
);
CREATE INDEX IF NOT EXISTS idx_bookings_room_dates   ON bookings(room_id, check_in_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_bookings_user         ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status       ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_created      ON bookings(created_at);
CREATE INDEX IF NOT EXISTS idx_bookings_checkin_date ON bookings(check_in_date);
CREATE INDEX IF NOT EXISTS idx_bookings_checkout_dt  ON bookings(check_out_date);
CREATE INDEX IF NOT EXISTS idx_bookings_approved_by  ON bookings(approved_by);
CREATE INDEX IF NOT EXISTS idx_bookings_payment_dl   ON bookings(payment_deadline);
CREATE INDEX IF NOT EXISTS idx_bookings_hold_expires ON bookings(hold_expires_at);

CREATE TABLE IF NOT EXISTS booking_status_transitions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id  INT NOT NULL,
    from_status VARCHAR(32),
    to_status   VARCHAR(32) NOT NULL,
    user_id     INT,
    reason      VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bst_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_bst_user    FOREIGN KEY (user_id)    REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_bst_booking        ON booking_status_transitions(booking_id);
CREATE INDEX IF NOT EXISTS idx_bst_booking_created ON booking_status_transitions(booking_id, created_at);
CREATE INDEX IF NOT EXISTS idx_bst_user           ON booking_status_transitions(user_id);

-- ----- Reviews -----
CREATE TABLE IF NOT EXISTS reviews (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    user_id            INT NOT NULL,
    room_id            INT NOT NULL,
    booking_id         INT,
    rating             INT NOT NULL,
    rating_cleanliness INT,
    rating_service     INT,
    rating_location    INT,
    rating_value       INT,
    rating_amenities   INT,
    comment            VARCHAR(1000),
    admin_reply        TEXT,
    admin_reply_at     TIMESTAMP NULL,
    report_count       INT NOT NULL DEFAULT 0,
    is_hidden          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP,
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_reviews_room    FOREIGN KEY (room_id)    REFERENCES rooms(id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);
CREATE INDEX IF NOT EXISTS idx_reviews_room   ON reviews(room_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user   ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews(rating);
CREATE INDEX IF NOT EXISTS idx_reviews_booking ON reviews(booking_id);

CREATE TABLE IF NOT EXISTS review_photos (
    review_id  INT NOT NULL,
    photo_url  VARCHAR(512),
    CONSTRAINT fk_review_photos FOREIGN KEY (review_id) REFERENCES reviews(id)
);

-- ----- Wishlist -----
CREATE TABLE IF NOT EXISTS wishlists (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    room_id    INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wishlists_user_room UNIQUE (user_id, room_id),
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wishlist_room FOREIGN KEY (room_id) REFERENCES rooms(id)
);
CREATE INDEX IF NOT EXISTS idx_wishlists_user ON wishlists(user_id);

-- ----- Payments -----
CREATE TABLE IF NOT EXISTS payments (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    booking_id        INT NOT NULL,
    amount            DECIMAL(12, 0) NOT NULL,
    method            VARCHAR(32) NOT NULL,
    status            VARCHAR(32) NOT NULL,
    gateway           VARCHAR(32),
    currency          VARCHAR(8)   DEFAULT 'VND',
    transaction_ref   VARCHAR(128) UNIQUE,
    intent_id         VARCHAR(128) UNIQUE,
    raw_response      TEXT,
    notes             TEXT,
    refund_amount     DECIMAL(12, 0),
    refund_reason     TEXT,
    processed_by      INT,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at      TIMESTAMP NULL,
    refunded_at       TIMESTAMP NULL,
    CONSTRAINT fk_payments_booking    FOREIGN KEY (booking_id)    REFERENCES bookings(id),
    CONSTRAINT fk_payments_processed_by FOREIGN KEY (processed_by) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_payments_booking    ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_status     ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_transaction ON payments(transaction_ref);
CREATE INDEX IF NOT EXISTS idx_payments_processed_by ON payments(processed_by);

-- ----- Notifications -----
CREATE TABLE IF NOT EXISTS notifications (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    VARCHAR(1000) NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    link       VARCHAR(512),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_notif_user   ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_unread ON notifications(user_id, is_read);

-- ----- Audit log -----
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    username    VARCHAR(255),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   INT,
    ip_address  VARCHAR(64),
    user_agent  VARCHAR(512),
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_user    ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action  ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_entity  ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);

-- ----- FAQ -----
CREATE TABLE IF NOT EXISTS faq_items (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    question         VARCHAR(512) NOT NULL,
    answer           TEXT NOT NULL,
    category         VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    display_order    INT NOT NULL DEFAULT 0,
    is_published     BOOLEAN NOT NULL DEFAULT TRUE,
    helpful_count    INT NOT NULL DEFAULT 0,
    not_helpful_count INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NULL,
    meta_keywords    VARCHAR(512)
);
CREATE INDEX IF NOT EXISTS idx_faq_category ON faq_items(category);
CREATE INDEX IF NOT EXISTS idx_faq_order    ON faq_items(display_order);

-- ----- Blogs -----
CREATE TABLE IF NOT EXISTS blogs (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    excerpt     TEXT,
    content     TEXT,
    image_url   VARCHAR(512),
    author      VARCHAR(128),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----- AI chat -----
CREATE TABLE IF NOT EXISTS chat_messages (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    user_message TEXT NOT NULL,
    ai_response  TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_chat_user    ON chat_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_created ON chat_messages(created_at);

-- ----- Contact form -----
CREATE TABLE IF NOT EXISTS contact_messages (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(64),
    subject     VARCHAR(255) NOT NULL,
    message     TEXT NOT NULL,
    type        VARCHAR(32) NOT NULL,
    priority    VARCHAR(32) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    assigned_to INT,
    admin_notes TEXT,
    ip_address  VARCHAR(64),
    user_agent  VARCHAR(512),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NULL,
    responded_at TIMESTAMP NULL,
    CONSTRAINT fk_contact_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_contact_status   ON contact_messages(status);
CREATE INDEX IF NOT EXISTS idx_contact_created  ON contact_messages(created_at);