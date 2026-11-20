package com.hsf.hotel.booking.service;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.promotion.model.Promotion;
import com.hsf.hotel.promotion.repository.PromotionRepository;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ADR-0009 booking policies against the real JPA layer (H2):
 * Q11 min/max stay, Q3 modification rules, natural idempotency, promotion release (R-20).
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookingPolicyIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private PromotionRepository promotionRepository;

    @MockBean private NotificationProducer notificationProducer;

    private User guest;
    private Room room;
    private LocalDate monday;

    @BeforeEach
    void setUp() {
        long s = System.nanoTime();
        guest = user("policy_" + s);
        room = room("POL-" + s, 2, 5);
        LocalDate d = LocalDate.now().plusDays(60);
        while (d.getDayOfWeek() != java.time.DayOfWeek.MONDAY) d = d.plusDays(1);
        monday = d;
    }

    @Test
    @DisplayName("Q11: overnight stay shorter than the villa minimum is rejected; day-use is exempt")
    void minimumStayEnforced() {
        assertThatThrownBy(() -> book(monday, monday.plusDays(1), 2))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode()).isEqualTo("MIN_STAY_NOT_MET");
        Booking dayUse = book(monday, monday, 2);
        assertThat(dayUse.getId()).isNotNull();
    }

    @Test
    @DisplayName("Q11: overnight stay longer than the villa maximum is rejected")
    void maximumStayEnforced() {
        assertThatThrownBy(() -> book(monday, monday.plusDays(6), 2))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode()).isEqualTo("MAX_STAY_EXCEEDED");
    }

    @Test
    @DisplayName("Natural idempotency: re-submitting the same stay returns the guest's existing hold")
    void duplicateSubmitReturnsSameHold() {
        Booking first = book(monday, monday.plusDays(2), 2);
        Booking replay = book(monday, monday.plusDays(2), 2);
        assertThat(replay.getId()).isEqualTo(first.getId());
        long holds = bookingRepository.findAllByRoomId(room.getId()).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT).count();
        assertThat(holds).isEqualTo(1);
    }

    @Test
    @DisplayName("Q3: a PENDING booking is fully re-priced when guests change (extra-guest fee included)")
    void pendingModificationReprices() {
        Booking b = book(monday, monday.plusDays(2), 2);
        BigDecimal before = b.getTotalPrice();
        Booking modified = bookingService.modifyBooking(b.getId(), guest, null, null, 4);
        assertThat(modified.getGuests()).isEqualTo(4);
        assertThat(modified.getTotalPrice()).isGreaterThan(before);
    }

    @Test
    @DisplayName("Q3: a PAID booking cannot change its total via self-service")
    void paidModificationThatChangesPriceIsRejected() {
        Booking b = book(monday, monday.plusDays(2), 2);
        b.setStatus(BookingStatus.PAID);
        bookingRepository.save(b);

        assertThatThrownBy(() -> bookingService.modifyBooking(b.getId(), guest, null, null, 4))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode()).isEqualTo("MODIFICATION_REQUIRES_SUPPORT");
        assertThat(bookingRepository.findById(b.getId()).orElseThrow().getGuests()).isEqualTo(2);
    }

    @Test
    @DisplayName("R-20: an expired hold returns its promotion use")
    void expiredHoldReleasesPromotionUse() {
        Promotion promo = new Promotion();
        promo.setTitle("Policy test " + System.nanoTime());
        promo.setDescription("10% off");
        promo.setCategory(Promotion.PromotionCategory.values()[0]);
        promo.setStartDate(LocalDate.now().minusDays(1));
        promo.setEndDate(LocalDate.now().plusDays(120));
        promo.setDiscountPercent(BigDecimal.TEN);
        promo.setMinimumNights(1);
        promo.setMinimumBookingAmount(BigDecimal.ZERO);
        promo.setMaximumUses(5);
        promo.setCurrentUses(0);
        promo.setIsActive(true);
        promo.setPromoCode("POL" + (System.nanoTime() % 1_000_000));
        promo = promotionRepository.save(promo);

        Booking b = bookingService.createBooking(guest, room, monday, monday.plusDays(2),
                guest.getUsername(), "0900000000", guest.getEmail(), 2, null, promo.getPromoCode());
        assertThat(promotionRepository.findById(promo.getId()).orElseThrow().getCurrentUses()).isEqualTo(1);

        bookingService.expireHold(b.getId());

        assertThat(promotionRepository.findById(promo.getId()).orElseThrow().getCurrentUses()).isEqualTo(0);
    }

    private Booking book(LocalDate in, LocalDate out, int guests) {
        return bookingService.createBooking(guest, room, in, out,
                guest.getUsername(), "0900000000", guest.getEmail(), guests, null, null);
    }

    private User user(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.local");
        u.setPasswordHash("$2a$10$dummy");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    private Room room(String number, int minStay, int maxStay) {
        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("Policy type " + number);
        type.setDescription("policy");
        type = roomTypeRepository.save(type);
        Room r = new Room();
        r.setRoomNumber(number);
        r.setRoomType(type);
        r.setPricePerNight(BigDecimal.valueOf(10_000_000));
        r.setCapacity(6);
        r.setBedrooms(3);
        r.setIsAvailable(true);
        r.setMinimumStay(minStay);
        r.setMaximumStay(maxStay);
        return roomRepository.save(r);
    }
}
