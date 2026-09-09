package com.hsf.hotel.room.dto;
import com.hsf.hotel.room.dto.RoomTypeDTO;
import com.hsf.hotel.room.dto.AmenityDTO;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomDTO implements java.io.Serializable {
    private Integer id;
    private String roomNumber;
    private RoomTypeDTO roomType;
    private List<AmenityDTO> amenities;
    private BigDecimal pricePerNight;
    private String description;
    private String imageUrl;
    private Boolean isAvailable;
    private Integer capacity;
    private Integer bedrooms;
    private BigDecimal avgRating;
    private Long reviewCount;
    private List<String> galleryImages;
    private String houseRules;
    private String policies;
    private String nearbyAttractions;
    private String checkInTime;
    private String checkOutTime;
    private Double latitude;
    private Double longitude;
    private Integer minimumStay;
    private Integer maximumStay;
    private String roomTypeDisplayName;
}
