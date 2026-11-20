package com.hsf.hotel.it;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.payment.repository.PaymentRepository;
import com.hsf.hotel.promotion.model.Promotion;
import com.hsf.hotel.promotion.repository.PromotionRepository;
import com.hsf.hotel.property.repository.PropertyRepository;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.review.repository.ReviewRepository;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import com.hsf.hotel.voucher.model.Voucher;
import com.hsf.hotel.voucher.repository.VoucherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P2 schema rules (V15–V20) on PostgreSQL. */
@RequiresPostgres
@DisplayName("P2 schema integrity on PostgreSQL")
class SchemaIntegrityIT extends AbstractPostgresIT {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingService bookingService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private PropertyRepository propertyRepository;

    @MockBean private NotificationProducer notificationProducer;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("V15: room slugs are unique")
    void slugUnique() {
        long s = System.nanoTime();
        roomRepository.saveAndFlush(room("SL-A-" + s, "same-slug-" + s));
        assertThatThrownBy(() -> roomRepository.saveAndFlush(room("SL-B-" + s, "same-slug-" + s)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uq_rooms_slug");
    }

    @Test
    @DisplayName("V19: audit columns record the acting user on create and update")
    void auditStamping() {
        User staff = user("audit_" + System.nanoTime(), UserRole.MANAGER);
        actAs(staff);
        Room saved = roomRepository.saveAndFlush(room("AU-" + System.nanoTime(), null));
        assertThat(saved.getCreatedBy()).isEqualTo(staff.getId());
        assertThat(saved.getCreatedAt()).isNotNull();

        saved.setDescription("changed");
        Room updated = roomRepository.saveAndFlush(saved);
        assertThat(updated.getUpdatedBy()).isEqualTo(staff.getId());
        assertThat(updated.getUpdatedAt()).isNotNull();
        Integer dbCreatedBy = jdbc.queryForObject("SELECT created_by FROM rooms WHERE id = ?", Integer.class, saved.getId());
        assertThat(dbCreatedBy).isEqualTo(staff.getId());
    }

    @Test
    @DisplayName("V20: the seeded property exists and new villas belong to it")
    void defaultProperty() {
        assertThat(propertyRepository.findByCode("NHU-DALAT")).isPresent();
        Room saved = roomRepository.saveAndFlush(room("PR-" + System.nanoTime(), null));
        Integer propertyId = jdbc.queryForObject("SELECT property_id FROM rooms WHERE id = ?", Integer.class, saved.getId());
        assertThat(propertyId).isEqualTo(1);
    }

    @Test
    @DisplayName("V16: a booking made with a voucher stores the typed voucher_id")
    void bookingStoresVoucherId() {
        long s = System.nanoTime();
        User guest = user("v16_" + s, UserRole.USER);
        Room room = roomRepository.saveAndFlush(room("V16-" + s, null));
        Voucher voucher = new Voucher();
        voucher.setCode("V16" + (s % 1_000_000));
        voucher.setAmount(BigDecimal.TEN);
        voucher.setPercent(true);
        voucher.setQuantity(5);
        voucher.setExpiryDate(LocalDate.now().plusDays(30));
        voucher = voucherRepository.saveAndFlush(voucher);

        LocalDate in = LocalDate.now().plusDays(120);
        Booking booking = bookingService.createBooking(guest, room, in, in.plusDays(2),
                guest.getUsername(), "0900000000", guest.getEmail(), 2, null, voucher.getCode());

        Integer voucherId = jdbc.queryForObject("SELECT voucher_id FROM bookings WHERE id = ?", Integer.class, booking.getId());
        assertThat(voucherId).isEqualTo(voucher.getId());
    }

    @Test
    @DisplayName("V17: ratings, money, enums and natural keys are enforced by the database")
    void domainChecks() {
        long s = System.nanoTime();
        User guest = user("v17_" + s, UserRole.USER);
        Room room = roomRepository.saveAndFlush(room("V17-" + s, null));

        // The entity already rejects rating 6; the CHECK protects writes that bypass it.
        Review standalone = new Review();
        standalone.setUser(guest);
        standalone.setRoom(room);
        standalone.setRating(5);
        Review savedStandalone = reviewRepository.saveAndFlush(standalone);
        assertThatThrownBy(() -> jdbc.update("UPDATE reviews SET rating = 6 WHERE id = ?", savedStandalone.getId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("chk_reviews_rating");

        Booking booking = new Booking();
        booking.setUser(guest);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now().plusDays(200));
        booking.setCheckOutDate(LocalDate.now().plusDays(202));
        booking.setStatus(com.hsf.hotel.booking.model.BookingStatus.PAID);
        booking.setTotalPrice(new BigDecimal("1000000"));
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        Payment negative = new Payment(savedBooking, new BigDecimal("-1"), Payment.PaymentMethod.CASH,
                Payment.PaymentStatus.PAID, "V17-NEG-" + s);
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(negative))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("chk_payments_amounts");

        Payment ok = paymentRepository.saveAndFlush(new Payment(savedBooking, new BigDecimal("1000000"),
                Payment.PaymentMethod.CASH, Payment.PaymentStatus.PAID, "V17-OK-" + s));
        assertThatThrownBy(() -> jdbc.update("UPDATE payments SET status = 'BOGUS' WHERE id = ?", ok.getId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("chk_payments_status");

        Review first = new Review();
        first.setUser(guest);
        first.setRoom(room);
        first.setBooking(savedBooking);
        first.setRating(5);
        reviewRepository.saveAndFlush(first);
        Review second = new Review();
        second.setUser(guest);
        second.setRoom(room);
        second.setBooking(savedBooking);
        second.setRating(4);
        assertThatThrownBy(() -> reviewRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("uq_reviews_booking");

        promotionRepository.saveAndFlush(promotion("dupe" + s));
        assertThatThrownBy(() -> promotionRepository.saveAndFlush(promotion("DUPE" + s)))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("uq_promotions_promo_code");
    }

    private void actAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        auth.setDetails(user.getId());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Promotion promotion(String code) {
        Promotion p = new Promotion();
        p.setTitle("IT " + code);
        p.setDescription("it");
        p.setCategory(Promotion.PromotionCategory.SEASONAL);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(10));
        p.setDiscountPercent(BigDecimal.TEN);
        p.setMinimumNights(1);
        p.setMinimumBookingAmount(BigDecimal.ZERO);
        p.setCurrentUses(0);
        p.setIsActive(true);
        p.setPromoCode(code);
        return p;
    }

    private User user(String username, UserRole role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@it.local");
        u.setPasswordHash("$2a$10$dummy");
        u.setRole(role);
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    private Room room(String number, String slug) {
        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("IT P2 type " + number);
        type.setDescription("integration");
        type = roomTypeRepository.save(type);
        Room r = new Room();
        r.setRoomNumber(number);
        r.setSlug(slug);
        r.setRoomType(type);
        r.setPricePerNight(new BigDecimal("10000000"));
        r.setCapacity(4);
        r.setBedrooms(2);
        r.setIsAvailable(true);
        return r;
    }
}
