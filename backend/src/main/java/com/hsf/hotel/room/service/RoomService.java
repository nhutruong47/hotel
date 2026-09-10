package com.hsf.hotel.room.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.room.repository.RoomRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms() {
        return roomRepository.findByIsAvailableTrue();
    }

    public long countAll() {
        return roomRepository.count();
    }

    public long countAvailable() {
        return roomRepository.countByIsAvailableTrue();
    }

    @Transactional(readOnly = true)
    public List<Room> getRoomsByType(RoomTypeEntity roomType) {
        return roomRepository.findByRoomType(roomType);
    }

    @Transactional(readOnly = true)
    public List<Room> searchRooms(LocalDate checkIn, LocalDate checkOut, BigDecimal minPrice,
                                  BigDecimal maxPrice, Integer roomTypeId,
                                  Integer capacity, Integer bedrooms, List<String> amenities,
                                  String promotion) {
        org.springframework.data.jpa.domain.Specification<Room> spec = org.springframework.data.jpa.domain.Specification
            .where(com.hsf.hotel.room.repository.RoomSpecification.isAvailable())
            .and(com.hsf.hotel.room.repository.RoomSpecification.priceBetween(minPrice, maxPrice))
            .and(com.hsf.hotel.room.repository.RoomSpecification.hasRoomType(roomTypeId))
            .and(com.hsf.hotel.room.repository.RoomSpecification.hasCapacityGreaterThanEqual(capacity))
            .and(com.hsf.hotel.room.repository.RoomSpecification.hasBedroomsGreaterThanEqual(bedrooms))
            .and(com.hsf.hotel.room.repository.RoomSpecification.hasAmenities(amenities))
            .and(com.hsf.hotel.room.repository.RoomSpecification.isNotBooked(checkIn, checkOut))
            .and(com.hsf.hotel.room.repository.RoomSpecification.hasPromotion(promotion));

        return roomRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Optional<Room> getRoomById(Integer id) {
        return roomRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Room> getRoomBySlug(String slug) {
        return roomRepository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public Optional<Room> getRoomByIdOrSlug(String idOrSlug) {
        if (idOrSlug == null || idOrSlug.isBlank()) return Optional.empty();
        try {
            int id = Integer.parseInt(idOrSlug);
            Optional<Room> byId = roomRepository.findById(id);
            if (byId.isPresent()) return byId;
        } catch (NumberFormatException ignored) {}
        Optional<Room> bySlug = roomRepository.findBySlug(idOrSlug);
        if (bySlug.isPresent()) return bySlug;
        return roomRepository.findByRoomNumber(idOrSlug);
    }

    @Transactional(readOnly = true)
    public boolean isRoomAvailableForDates(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!Boolean.TRUE.equals(room.getIsAvailable())) {
            return false;
        }
        return bookingRepository.findConflictingBookings(room, checkIn, checkOut).isEmpty();
    }

    @CacheEvict(cacheNames = {"rooms", "roomViews"}, allEntries = true)
    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }

    @Transactional
    @CacheEvict(cacheNames = {"rooms", "roomViews"}, allEntries = true)
    public void deleteRoom(Integer id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        var bookings = bookingRepository.findAllByRoomId(id);
        if (bookings != null && !bookings.isEmpty()) {
            throw new BusinessRuleException("ROOM_HAS_HISTORY",
                    "Phòng này đã có lịch sử đặt phòng, không thể xóa. Vui lòng tắt trạng thái 'Còn trống' để ẩn phòng.");
        }
        roomRepository.delete(room);
    }
}
