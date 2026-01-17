package com.hsf.hotel.service;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomType;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findByIsAvailableTrue();
    }

    public List<Room> getRoomsByType(RoomType roomType) {
        return roomRepository.findByRoomType(roomType);
    }

    public Optional<Room> getRoomById(Integer id) {
        return roomRepository.findById(id);
    }

    public boolean isRoomAvailableForDates(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!room.getIsAvailable()) {
            return false;
        }
        return bookingRepository.findConflictingBookings(room, checkIn, checkOut).isEmpty();
    }

    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }
}
