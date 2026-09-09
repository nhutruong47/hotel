package com.hsf.hotel.room.service;

import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.room.dto.RoomDTO;
import com.hsf.hotel.room.dto.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Transactional read boundary for room APIs. Mapping happens while the JPA
 * session is open and only DTOs are cached, preventing detached lazy proxies.
 */
@Service
@RequiredArgsConstructor
public class RoomQueryService {
    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "roomViews", key = "'available'")
    public List<RoomDTO> available() {
        return roomMapper.roomsToRoomDTOs(roomService.getAvailableRooms());
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> search(
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer roomTypeId,
            Integer capacity,
            Integer bedrooms,
            List<String> amenities,
            String promotion) {
        return roomMapper.roomsToRoomDTOs(roomService.searchRooms(
                checkIn, checkOut, minPrice, maxPrice, roomTypeId,
                capacity, bedrooms, amenities, promotion));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "roomViews", key = "'detail_' + #id")
    public RoomDTO byId(Integer id) {
        return roomMapper.roomToRoomDTO(roomService.getRoomById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id)));
    }
}
