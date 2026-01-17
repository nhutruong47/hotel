package com.hsf.hotel.service;

import com.hsf.hotel.model.*;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<Booking> getBookingById(Integer id) {
        return bookingRepository.findById(id);
    }

    public BigDecimal calculateTotalPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    public boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!room.getIsAvailable()) {
            return false;
        }
        return bookingRepository.findConflictingBookings(room, checkIn, checkOut).isEmpty();
    }

    @Transactional
    public Booking createBooking(User user, Room room, LocalDate checkIn, LocalDate checkOut,
            String guestName, String guestPhone, String notes) {
        // Check availability
        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new RuntimeException("Phòng không khả dụng trong khoảng thời gian này");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestName(guestName);
        booking.setGuestPhone(guestPhone);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalPrice(calculateTotalPrice(room, checkIn, checkOut));

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Integer bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn này");
        }

        // Check if can cancel
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Đơn đặt phòng đã được hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy đơn đã hoàn thành");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}
