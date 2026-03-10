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

        @Query("SELECT b FROM Booking b WHERE b.checkInDate >= :startDate AND b.checkInDate <= :endDate AND b.status IN ('CONFIRMED', 'COMPLETED')")
        List<Booking> findSuccessfulBookingsInDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Check if room is available for given date range
        @Query("SELECT b FROM Booking b WHERE b.room = :room " +
                        "AND b.status NOT IN ('CANCELLED') " +
                        "AND ((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn))")
        List<Booking> findConflictingBookings(
                        @Param("room") Room room,
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut);

        List<Booking> findAllByCheckInDateBetween(LocalDate startDate, LocalDate endDate);

        @Query("SELECT b.room, COUNT(b.room) as booking_count FROM Booking b GROUP BY b.room ORDER BY booking_count DESC")
        List<Object[]> findMostBookedRooms();

        @Query("SELECT b.room, AVG(r.rating) as average_rating FROM Booking b JOIN Review r ON b.id = r.booking.id GROUP BY b.room ORDER BY average_rating DESC")
        List<Object[]> findMostratingRooms();

        List<Booking> findAllByRoomId(Integer roomId);

        @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
                        "AND b.status NOT IN ('CANCELLED') " +
                        "AND b.checkOutDate >= CURRENT_DATE " +
                        "ORDER BY b.checkInDate ASC")
        List<Booking> findActiveBookingsByRoomId(@Param("roomId") Integer roomId);

        @Query("SELECT b FROM Booking b WHERE b.status = 'AWAITING_PAYMENT' AND b.paymentDeadline IS NOT NULL AND b.paymentDeadline < :now")
        List<Booking> findExpiredPaymentBookings(@Param("now") java.time.LocalDateTime now);
}
