package com.hsf.hotel.it;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.payment.model.PaymentRefund;
import com.hsf.hotel.payment.repository.PaymentRefundRepository;
import com.hsf.hotel.payment.repository.PaymentRepository;
import com.hsf.hotel.payment.service.PaymentService;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ADR-0009 Q5/Q9 end-to-end on PostgreSQL: a bank-transfer refund stays
 * PENDING_MANUAL (bank details retained) until ADMIN confirms it with a bank
 * reference; after the retention period the guest's bank details are erased.
 */
@RequiresPostgres
@DisplayName("Manual refund workflow + bank-detail retention on PostgreSQL")
class ManualRefundIT extends AbstractPostgresIT {

    @Autowired private PaymentService paymentService;
    @Autowired private BookingService bookingService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentRefundRepository refundRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @MockBean private NotificationProducer notificationProducer;

    @Test
    void manualRefundLifecycleAndBankDetailPurge() {
        long s = System.nanoTime();
        User guest = user("mr_guest_" + s, UserRole.USER);
        User admin = user("mr_admin_" + s, UserRole.ADMIN);
        Room room = room("MR-" + s);

        Booking booking = new Booking();
        booking.setUser(guest);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now().plusDays(30));
        booking.setCheckOutDate(LocalDate.now().plusDays(32));
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setTotalPrice(new BigDecimal("20000000"));
        booking.setGuestName("Guest " + s);
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        booking.setCancelledAt(LocalDateTime.now().minusDays(200));
        booking.setRefundBankName("VCB");
        booking.setRefundAccountNumber("0011223344");
        booking.setRefundAccountName("GUEST NAME");
        booking = bookingRepository.save(booking);

        Payment payment = new Payment(booking, new BigDecimal("20000000"), Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PAID, "IT-MR-" + s);
        payment.setGateway(Payment.PaymentGateway.SEPAY);
        payment = paymentRepository.save(payment);

        // 1. Refund request on a bank-transfer payment -> PENDING_MANUAL, no money "moved".
        paymentService.refund(payment.getId(), new BigDecimal("20000000"), "Hủy theo chính sách", admin, "it-mr-" + s);
        Payment afterRequest = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(afterRequest.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(afterRequest.getRefundAmount()).isEqualByComparingTo("0");
        PaymentRefund pending = refundRepository.findByPaymentIdAndIdempotencyKey(payment.getId(), "it-mr-" + s)
                .orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(PaymentRefund.RefundStatus.PENDING_MANUAL);

        // 2. Retention sweep must keep bank details while the refund is still pending.
        bookingService.purgeExpiredRefundBankDetails();
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getRefundAccountNumber())
                .isEqualTo("0011223344");

        // 3. ADMIN confirms the transfer -> payment REFUNDED, refund SUCCEEDED with reference.
        paymentService.confirmManualRefund(payment.getId(), pending.getId(), "VCB-IT-001", admin);
        Payment refunded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(refunded.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(refunded.getRefundAmount()).isEqualByComparingTo("20000000");
        PaymentRefund done = refundRepository.findById(pending.getId()).orElseThrow();
        assertThat(done.getStatus()).isEqualTo(PaymentRefund.RefundStatus.SUCCEEDED);
        assertThat(done.getBankReference()).isEqualTo("VCB-IT-001");

        // 4. Now the 180-day-old cancellation's bank details are erased.
        bookingService.purgeExpiredRefundBankDetails();
        Booking purged = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(purged.getRefundAccountNumber()).isNull();
        assertThat(purged.getRefundBankName()).isNull();
        assertThat(purged.getRefundAmount()).isEqualByComparingTo("20000000");
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

    private Room room(String number) {
        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("IT MR type " + number);
        type.setDescription("integration");
        type = roomTypeRepository.save(type);
        Room r = new Room();
        r.setRoomNumber(number);
        r.setRoomType(type);
        r.setPricePerNight(new BigDecimal("10000000"));
        r.setCapacity(4);
        r.setBedrooms(2);
        r.setIsAvailable(true);
        return roomRepository.save(r);
    }
}
