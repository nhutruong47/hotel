package com.hsf.hotel.repository;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findByIsAvailableTrue();

    List<Room> findByRoomType(RoomType roomType);

    List<Room> findByRoomTypeAndIsAvailableTrue(RoomType roomType);
}
