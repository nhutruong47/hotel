package com.hsf.hotel.service;

import com.hsf.hotel.model.RoomTypeEntity;
import com.hsf.hotel.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomTypeService {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    public List<RoomTypeEntity> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public Optional<RoomTypeEntity> getRoomTypeById(Integer id) {
        return roomTypeRepository.findById(id);
    }

    public RoomTypeEntity saveRoomType(RoomTypeEntity roomType) {
        return roomTypeRepository.save(roomType);
    }

    public void deleteRoomType(Integer id) {
        roomTypeRepository.deleteById(id);
    }
}
