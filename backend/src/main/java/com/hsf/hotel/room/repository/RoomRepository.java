package com.hsf.hotel.room.repository;
import com.hsf.hotel.booking.model.Booking;

import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer>, JpaSpecificationExecutor<Room> {
    List<Room> findByIsAvailableTrue();
    long countByIsAvailableTrue();
    java.util.Optional<Room> findBySlug(String slug);
    java.util.Optional<Room> findByRoomNumber(String roomNumber);


    List<Room> findByRoomType(RoomTypeEntity roomType);

    List<Room> findByRoomTypeAndIsAvailableTrue(RoomTypeEntity roomType);

    @Query("SELECT r FROM Room r WHERE r.isAvailable = true " +
            "AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice) " +
            "AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice) " +
            "AND (:roomTypeId IS NULL OR r.roomType.id = :roomTypeId) " +
            "AND (CAST(:checkIn AS date) IS NULL OR CAST(:checkOut AS date) IS NULL OR NOT EXISTS (" +
            "    SELECT b FROM Booking b WHERE b.room = r " +
            "    AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW') " +
            "    AND b.checkInDate < :checkOut " +
            "    AND b.checkOutDate > :checkIn" +
            "))")
    List<Room> findAvailableRooms(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("roomTypeId") Integer roomTypeId);
}
