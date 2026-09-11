package com.hsf.hotel.room.repository;

import com.hsf.hotel.room.model.VillaInspection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VillaInspectionRepository extends JpaRepository<VillaInspection, Integer> {

    @EntityGraph(attributePaths = {"room", "inspector"})
    List<VillaInspection> findByRoomIdOrderByScheduledDateDesc(Integer roomId);

    @EntityGraph(attributePaths = {"room", "inspector"})
    List<VillaInspection> findAllByOrderByScheduledDateDesc();

    @EntityGraph(attributePaths = {"room", "inspector"})
    List<VillaInspection> findByStatusOrderByScheduledDateDesc(VillaInspection.InspectionStatus status);

    @EntityGraph(attributePaths = {"room", "inspector"})
    List<VillaInspection> findByInspectionTypeOrderByScheduledDateDesc(VillaInspection.InspectionType type);

    @EntityGraph(attributePaths = {"room", "inspector"})
    @Query("SELECT i FROM VillaInspection i WHERE i.room.id = :roomId ORDER BY i.scheduledDate DESC LIMIT 1")
    Optional<VillaInspection> findLatestByRoomId(@Param("roomId") Integer roomId);

    long countByStatus(VillaInspection.InspectionStatus status);

    @Query("SELECT COUNT(i) FROM VillaInspection i WHERE i.droneModel IS NOT NULL AND i.droneModel != ''")
    long countDroneMissions();
}
