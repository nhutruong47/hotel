package com.hsf.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal pricePerNight;

    @Column(length = 500)
    private String description;

    private String imageUrl;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    public Room() {
    }

    public Room(Integer id, String roomNumber, RoomType roomType, BigDecimal pricePerNight,
            String description, String imageUrl, Boolean isAvailable) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.description = description;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    // Helper method
    public String getRoomTypeDisplayName() {
        return roomType != null ? roomType.getDisplayName() : "";
    }
}
