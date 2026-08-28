package com.hsf.hotel.repository;

import com.hsf.hotel.model.RoomTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, Integer> {
    Optional<RoomTypeEntity> findByName(String name);
}
