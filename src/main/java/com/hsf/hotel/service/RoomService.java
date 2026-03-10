package com.hsf.hotel.service;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomTypeEntity;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<Room> getRoomsByType(RoomTypeEntity roomType) {
        return roomRepository.findByRoomType(roomType);
    }

    public List<Room> searchRooms(LocalDate checkIn, LocalDate checkOut, java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice, Integer roomTypeId) {
        return roomRepository.findAvailableRooms(checkIn, checkOut, minPrice, maxPrice, roomTypeId);
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

    @Transactional
    public void deleteRoom(Integer id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Check if room has any bookings (past, present, or future)
        // Note: we just need to know if any bookings exist, so checking if the list is
        // not empty
        var bookings = bookingRepository.findAllByRoomId(id);
        if (bookings != null && !bookings.isEmpty()) {
            throw new RuntimeException(
                    "This room has booking history and cannot be deleted. Please toggle 'Available' status to hide it.");
        }

        roomRepository.delete(room);
    }

}
