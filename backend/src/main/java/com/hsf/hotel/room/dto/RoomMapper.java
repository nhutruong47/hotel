package com.hsf.hotel.room.dto;
import com.hsf.hotel.room.dto.RoomTypeDTO;
import com.hsf.hotel.room.dto.RoomDTO;
import com.hsf.hotel.room.dto.AmenityDTO;

import com.hsf.hotel.room.model.Amenity;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomMapper INSTANCE = Mappers.getMapper(RoomMapper.class);

    @Mapping(target = "roomTypeDisplayName", source = "roomType.displayName")
    RoomDTO roomToRoomDTO(Room room);

    List<RoomDTO> roomsToRoomDTOs(List<Room> rooms);

    RoomTypeDTO roomTypeToRoomTypeDTO(RoomTypeEntity roomType);
    
    List<RoomTypeDTO> roomTypesToRoomTypeDTOs(List<RoomTypeEntity> roomTypes);

    AmenityDTO amenityToAmenityDTO(Amenity amenity);
}
