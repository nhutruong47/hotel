package com.hsf.hotel.admin.dto;

import com.hsf.hotel.room.model.Amenity;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.voucher.model.Voucher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AdminResponseDtos {

    public record AmenityDto(
            Integer id,
            String name,
            String iconCode
    ) {
        public static AmenityDto from(Amenity a) {
            if (a == null) return null;
            return new AmenityDto(a.getId(), a.getName(), a.getIconCode());
        }
    }

    public record RoomTypeDto(
            Integer id,
            String name,
            String description
    ) {
        public static RoomTypeDto from(RoomTypeEntity rt) {
            if (rt == null) return null;
            return new RoomTypeDto(rt.getId(), rt.getName(), rt.getDescription());
        }
    }

    public record AdminRoomResponse(
            Integer id,
            String roomNumber,
            String slug,
            RoomTypeDto roomType,
            BigDecimal pricePerNight,
            String description,
            String imageUrl,
            Boolean isAvailable,
            Integer capacity,
            Integer bedrooms,
            BigDecimal avgRating,
            Long reviewCount,
            List<String> galleryImages,
            String houseRules,
            String policies,
            String nearbyAttractions,
            String checkInTime,
            String checkOutTime,
            Double latitude,
            Double longitude,
            String nearbyRestaurants,
            String nearbyCafes,
            String nearbyAirport,
            String directions,
            Integer minimumStay,
            Integer maximumStay,
            List<AmenityDto> amenities
    ) {
        public static AdminRoomResponse from(Room r) {
            if (r == null) return null;
            return new AdminRoomResponse(
                    r.getId(),
                    r.getRoomNumber(),
                    r.getSlug(),
                    RoomTypeDto.from(r.getRoomType()),
                    r.getPricePerNight(),
                    r.getDescription(),
                    r.getImageUrl(),
                    r.getIsAvailable(),
                    r.getCapacity(),
                    r.getBedrooms(),
                    r.getAvgRating(),
                    r.getReviewCount(),
                    r.getGalleryImages() != null ? List.copyOf(r.getGalleryImages()) : List.of(),
                    r.getHouseRules(),
                    r.getPolicies(),
                    r.getNearbyAttractions(),
                    r.getCheckInTime(),
                    r.getCheckOutTime(),
                    r.getLatitude(),
                    r.getLongitude(),
                    r.getNearbyRestaurants(),
                    r.getNearbyCafes(),
                    r.getNearbyAirport(),
                    r.getDirections(),
                    r.getMinimumStay(),
                    r.getMaximumStay(),
                    r.getAmenities() != null ? r.getAmenities().stream().map(AmenityDto::from).toList() : List.of()
            );
        }
    }

    public record AdminUserResponse(
            Integer id,
            String username,
            String email,
            String fullName,
            String role,
            Boolean emailVerified,
            String avatarFilename,
            String phone,
            String dateOfBirth,
            String gender,
            String nationality,
            String address,
            String city,
            String country,
            Boolean disabled
    ) {
        public static AdminUserResponse from(User u) {
            if (u == null) return null;
            return new AdminUserResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getFullName(),
                    u.getRole(),
                    u.getEmailVerified(),
                    u.getAvatarFilename(),
                    u.getPhone(),
                    u.getDateOfBirth(),
                    u.getGender(),
                    u.getNationality(),
                    u.getAddress(),
                    u.getCity(),
                    u.getCountry(),
                    u.getDisabled()
            );
        }
    }

    public record AdminVoucherResponse(
            Integer id,
            String code,
            BigDecimal amount,
            LocalDate expiryDate,
            Integer quantity,
            Integer usedCount,
            Boolean percent
    ) {
        public static AdminVoucherResponse from(Voucher v) {
            if (v == null) return null;
            return new AdminVoucherResponse(
                    v.getId(),
                    v.getCode(),
                    v.getAmount(),
                    v.getExpiryDate(),
                    v.getQuantity(),
                    v.getUsedCount(),
                    v.getPercent()
            );
        }
    }
}
