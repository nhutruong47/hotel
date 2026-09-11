package com.hsf.hotel.wishlist.dto;

import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.wishlist.model.WishlistItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Stable public shape that never serializes the owning User entity. */
public record WishlistItemResponse(
        Integer id,
        RoomSummary room,
        LocalDateTime createdAt
) {
    public static WishlistItemResponse from(WishlistItem item) {
        return new WishlistItemResponse(item.getId(), RoomSummary.from(item.getRoom()), item.getCreatedAt());
    }

    public record RoomSummary(
            Integer id,
            String roomNumber,
            BigDecimal pricePerNight,
            String imageUrl,
            String description,
            RoomTypeSummary roomType,
            Integer capacity,
            BigDecimal avgRating
    ) {
        private static RoomSummary from(Room room) {
            return new RoomSummary(
                    room.getId(),
                    room.getRoomNumber(),
                    room.getPricePerNight(),
                    room.getImageUrl(),
                    room.getDescription(),
                    RoomTypeSummary.from(room.getRoomType()),
                    room.getCapacity(),
                    room.getAvgRating()
            );
        }
    }

    public record RoomTypeSummary(Integer id, String displayName) {
        private static RoomTypeSummary from(RoomTypeEntity type) {
            return type == null ? null : new RoomTypeSummary(type.getId(), type.getDisplayName());
        }
    }
}
