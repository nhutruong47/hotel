package com.hsf.hotel.booking.repository;
import com.hsf.hotel.review.model.Review;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.user.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import com.hsf.hotel.room.dto.RoomStatsDto;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT b FROM Booking b WHERE b.id = :id")
        Optional<Booking> findByIdForUpdate(@Param("id") Integer id);

        /**
         * Eagerly loads the {@code user} and {@code room} associations in a
         * single query so that the SPA can render the booking list without
         * triggering N+1 lookups. Use this on hot read paths where the
         * associations are known to be needed (admin lists, dashboards).
         */
        @EntityGraph(attributePaths = {"user", "room"})
        List<Booking> findByUserOrderByCreatedAtDesc(User user);

        @EntityGraph(attributePaths = {"room"})
        List<Booking> findByUserAndStatusOrderByCreatedAtDesc(User user, BookingStatus status);

        // Admin queries
        @EntityGraph(attributePaths = {"user", "room"})
        List<Booking> findAllByOrderByCreatedAtDesc();

        @EntityGraph(attributePaths = {"user", "room"})
        org.springframework.data.domain.Page<Booking> findAll(org.springframework.data.domain.Pageable pageable);

        @EntityGraph(attributePaths = {"user", "room"})
        org.springframework.data.domain.Page<Booking> findByStatus(BookingStatus status, org.springframework.data.domain.Pageable pageable);

        List<Booking> findByCheckInDate(LocalDate checkInDate);

        List<Booking> findByCheckOutDate(LocalDate checkOutDate);
        @EntityGraph(attributePaths = {"user", "room"})
        List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

        long countByStatus(BookingStatus status);

        @EntityGraph(attributePaths = {"user", "room"})
        @Query("SELECT b FROM Booking b WHERE b.checkInDate >= :startDate AND b.checkInDate <= :endDate")
        List<Booking> findBookingsInDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @EntityGraph(attributePaths = {"user", "room"})
        @Query("SELECT b FROM Booking b WHERE b.checkInDate >= :startDate AND b.checkInDate <= :endDate AND b.status IN ('PAID', 'CHECKED_IN', 'CHECKED_OUT', 'COMPLETED')")
        List<Booking> findSuccessfulBookingsInDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Check if room is available for given date range.
        // Any status that frees the dates is excluded. CANCELLED/EXPIRED/NO_SHOW
        // all represent inventory that has been released; we keep PAID,
        // CHECKED_IN, CHECKED_OUT, COMPLETED, and PENDING_PAYMENT (active holds).
        @Query("SELECT b FROM Booking b WHERE b.room = :room " +
                        "AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW') " +
                        "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
        List<Booking> findConflictingBookings(
                        @Param("room") Room room,
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT b FROM Booking b WHERE b.room = :room " +
                        "AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW') " +
                        "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
        List<Booking> findConflictingBookingsForUpdate(
                        @Param("room") Room room,
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut);

        /**
         * Used during booking modification so the booking does not conflict
         * with itself. Same status exclusion as
         * {@link #findConflictingBookingsForUpdate(Room, LocalDate, LocalDate)}.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT b FROM Booking b WHERE b.room = :room " +
                        "AND b.id <> :excludeBookingId " +
                        "AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW') " +
                        "AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)")
        List<Booking> findConflictingBookingsForUpdateExcluding(
                        @Param("room") Room room,
                        @Param("excludeBookingId") Integer excludeBookingId,
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut);

        List<Booking> findAllByCheckInDateBetween(LocalDate startDate, LocalDate endDate);

        @Query("SELECT b.room.id as roomId, b.room.roomNumber as roomNumber, COUNT(b.room) as totalBookings FROM Booking b GROUP BY b.room.id, b.room.roomNumber ORDER BY totalBookings DESC")
        List<RoomStatsDto> findMostBookedRooms();

        @Query("SELECT b.room.id as roomId, b.room.roomNumber as roomNumber, AVG(r.rating) as averageRating FROM Booking b JOIN Review r ON b.id = r.booking.id GROUP BY b.room.id, b.room.roomNumber ORDER BY averageRating DESC")
        List<RoomStatsDto> findMostratingRooms();

        List<Booking> findAllByRoomId(Integer roomId);

        @EntityGraph(attributePaths = {"room"})
        List<Booking> findByUserId(Integer userId);

        @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING_PAYMENT' AND b.paymentDeadline < :now")
        List<Booking> findExpiredPaymentBookings(@Param("now") LocalDateTime now);

        @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING_PAYMENT' AND b.holdExpiresAt < :now")
        List<Booking> findExpiredHoldBookings(@Param("now") LocalDateTime now);

        @EntityGraph(attributePaths = {"user"})
        @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
                        "AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW') " +
                        "AND b.checkOutDate >= CURRENT_DATE " +
                        "ORDER BY b.checkInDate ASC")
        List<Booking> findActiveBookingsByRoomId(@Param("roomId") Integer roomId);

        @EntityGraph(attributePaths = {"user", "room"})
        Optional<Booking> findWithDetailsById(Integer id);

        /**
         * Returns rows of [date(YYYY-MM-DD), daily_revenue] for the requested
         * range. Only counts bookings whose status represents realised
         * revenue (PAID/CHECKED_IN/CHECKED_OUT/COMPLETED).
         */
        @Query(value = "SELECT CAST(b.check_in_date AS DATE) AS booking_day, " +
                        "COALESCE(SUM(b.total_price), 0) AS daily_revenue " +
                        "FROM bookings b " +
                        "WHERE b.status IN ('PAID','CHECKED_IN','CHECKED_OUT','COMPLETED') " +
                        "AND b.check_in_date >= :startDate " +
                        "AND b.check_in_date <= :endDate " +
                        "GROUP BY CAST(b.check_in_date AS DATE) " +
                        "ORDER BY booking_day ASC", nativeQuery = true)
        List<Object[]> aggregateRevenueByDay(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Bookings overlapping a date that hold inventory (i.e. anything that
         * blocks the room from being sold to another guest). Used to compute
         * occupancy % per day.
         */
        @Query(value = "SELECT CAST(b.check_in_date AS DATE) AS booking_day " +
                        "FROM bookings b " +
                        "WHERE b.status IN ('PENDING_PAYMENT','PAID','CHECKED_IN','CHECKED_OUT','COMPLETED') " +
                        "AND b.check_in_date <= :endDate " +
                        "AND b.check_out_date >= :startDate", nativeQuery = true)
        List<Object[]> findOccupancyRowsInRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /** Bookings with non-null refund_amount (i.e. refunds processed). */
        @EntityGraph(attributePaths = {"user", "room"})
        @Query("SELECT b FROM Booking b WHERE b.refundAmount IS NOT NULL AND b.refundAmount > 0 " +
                        "ORDER BY b.modifiedAt DESC")
        List<Booking> findBookingsWithRefunds();
}
