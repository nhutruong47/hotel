package com.hsf.hotel.room.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable public contract for the searchable room catalogue. */
public record RoomCatalogResponse(
        List<RoomDTO> rooms,
        List<RoomTypeDTO> roomTypes,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
    public RoomCatalogResponse {
        rooms = List.copyOf(rooms);
        roomTypes = List.copyOf(roomTypes);
    }

    public static RoomCatalogResponse from(
            Page<RoomDTO> rooms,
            List<RoomTypeDTO> roomTypes,
            String sort
    ) {
        return new RoomCatalogResponse(
                rooms.getContent(),
                roomTypes,
                rooms.getNumber(),
                rooms.getSize(),
                rooms.getTotalElements(),
                rooms.getTotalPages(),
                rooms.isFirst(),
                rooms.isLast(),
                sort
        );
    }
}
