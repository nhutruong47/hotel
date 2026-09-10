package com.hsf.hotel.room.repository;

import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.VillaMaintenance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VillaMaintenanceRepository extends JpaRepository<VillaMaintenance, Integer> {

    @EntityGraph(attributePaths = {"room", "createdBy"})
    List<VillaMaintenance> findByRoomIdOrderByStartDateDesc(Integer roomId);

    @EntityGraph(attributePaths = {"room", "createdBy"})
    List<VillaMaintenance> findAllByOrderByStartDateDesc();

    @Query("SELECT m FROM VillaMaintenance m WHERE m.room = :room " +
           "AND m.status IN ('SCHEDULED', 'IN_PROGRESS') " +
           "AND (m.startDate < :checkOut AND m.endDate > :checkIn)")
    List<VillaMaintenance> findConflictingMaintenances(
            @Param("room") Room room,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    @Query("SELECT m FROM VillaMaintenance m WHERE m.room.id = :roomId " +
           "AND m.status IN ('SCHEDULED', 'IN_PROGRESS') " +
           "AND m.endDate >= :today " +
           "ORDER BY m.startDate ASC")
    List<VillaMaintenance> findActiveAndUpcomingByRoomId(
            @Param("roomId") Integer roomId,
            @Param("today") LocalDate today);
}
