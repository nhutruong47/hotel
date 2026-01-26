package com.hsf.hotel.controller;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller cho realtime notifications sử dụng Server-Sent Events (SSE)
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private BookingRepository bookingRepository;

    // Danh sách các SSE connections đang active
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * SSE endpoint cho admin - nhận realtime updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Gửi initial data
        try {
            long pendingCount = bookingRepository.countByStatus(BookingStatus.PENDING);
            long awaitingPaymentCount = bookingRepository.countByStatus(BookingStatus.AWAITING_PAYMENT);

            emitter.send(SseEmitter.event()
                    .name("init")
                    .data(Map.of(
                            "pendingCount", pendingCount,
                            "awaitingPaymentCount", awaitingPaymentCount,
                            "time", LocalDateTime.now().toString())));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * API để gửi notification cho tất cả connected admins
     */
    public void broadcastPaymentNotification(Booking booking) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("payment")
                        .data(Map.of(
                                "bookingId", booking.getId(),
                                "roomNumber", booking.getRoom().getRoomNumber(),
                                "guestName", booking.getGuestName(),
                                "amount", booking.getTotalPrice().toString(),
                                "paidAt", booking.getPaidAt().toString(),
                                "message", "Booking #" + booking.getId() + " đã thanh toán!")));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    /**
     * API để gửi notification khi có booking mới
     */
    public void broadcastNewBookingNotification(Booking booking) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("newBooking")
                        .data(Map.of(
                                "bookingId", booking.getId(),
                                "roomNumber", booking.getRoom().getRoomNumber(),
                                "guestName", booking.getGuestName(),
                                "message", "Booking mới #" + booking.getId() + " cần duyệt!")));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Polling endpoint (backup cho browsers không support SSE)
     */
    @GetMapping("/poll")
    public Map<String, Object> pollNotifications() {
        long pendingCount = bookingRepository.countByStatus(BookingStatus.PENDING);
        long awaitingPaymentCount = bookingRepository.countByStatus(BookingStatus.AWAITING_PAYMENT);
        long confirmedCount = bookingRepository.countByStatus(BookingStatus.CONFIRMED);

        return Map.of(
                "pendingCount", pendingCount,
                "awaitingPaymentCount", awaitingPaymentCount,
                "confirmedCount", confirmedCount,
                "time", LocalDateTime.now().toString());
    }
}
