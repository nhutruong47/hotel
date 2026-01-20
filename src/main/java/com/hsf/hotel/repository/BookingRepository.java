package com.hsf.hotel.repository;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
        List<Booking> findByUserOrderByCreatedAtDesc(User user);

        List<Booking> findByUserAndStatusOrderByCreatedAtDesc(User user, BookingStatus status);

        // Admin queries
        List<Booking> findAllByOrderByCreatedAtDesc();

        List<Booking> findByCheckInDate(LocalDate checkInDate);

        List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

        long countByStatus(BookingStatus status);

        @Query("SELECT b FROM Booking b WHERE b.checkInDate >= :startDate AND b.checkInDate <= :endDate")
        List<Booking> findBookingsInDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Check if room is available for given date range
        @Query("SELECT b FROM Booking b WHERE b.room = :room " +
                        "AND b.status NOT IN ('CANCELLED', 'REJECTED') " +
                        "AND ((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn))")
        List<Booking> findConflictingBookings(
                        @Param("room") Room room,
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut);

        // Find bookings with expired payment deadline
        @Query("SELECT b FROM Booking b WHERE b.status = 'AWAITING_PAYMENT' " +
                        "AND b.paymentDeadline IS NOT NULL AND b.paymentDeadline < :now")
        List<Booking> findExpiredPaymentBookings(@Param("now") LocalDateTime now);
}
