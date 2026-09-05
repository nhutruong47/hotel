package com.hsf.hotel.room.service;

import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "roomTypes", key = "'all'")
    public List<RoomTypeEntity> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RoomTypeEntity> getRoomTypeById(Integer id) {
        return roomTypeRepository.findById(id);
    }

    @CacheEvict(cacheNames = "roomTypes", allEntries = true)
    public RoomTypeEntity saveRoomType(RoomTypeEntity roomType) {
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    @CacheEvict(cacheNames = "roomTypes", allEntries = true)
    public void deleteRoomType(Integer id) {
        roomTypeRepository.deleteById(id);
    }
}
