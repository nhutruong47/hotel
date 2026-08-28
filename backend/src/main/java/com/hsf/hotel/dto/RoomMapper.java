package com.hsf.hotel.dto;

import com.hsf.hotel.model.Amenity;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomTypeEntity;
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
