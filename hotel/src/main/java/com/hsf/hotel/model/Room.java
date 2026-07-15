package com.hsf.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_rooms_available", columnList = "isAvailable"),
        @Index(name = "idx_rooms_room_type", columnList = "room_type_id"),
        @Index(name = "idx_rooms_room_number", columnList = "roomNumber", unique = true)
})
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String roomNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private List<Amenity> amenities = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal pricePerNight;

    @Column(length = 500)
    private String description;

    private String imageUrl;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    /** Maximum number of guests the villa can accommodate. */
    @Column(nullable = false)
    private Integer capacity = 2;

    /** Number of bedrooms. */
    @Column(nullable = false)
    private Integer bedrooms = 1;

    /** Average rating (denormalised for fast reads; reviews are the source of truth). */
    @Column(precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    /** Total review count (denormalised for fast reads). */
    @Column(nullable = false)
    private Long reviewCount = 0L;

    /** Multiple gallery images for the villa */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_gallery_images", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "image_url", length = 500)
    private List<String> galleryImages = new ArrayList<>();

    /** House rules displayed on villa detail page */
    @Column(columnDefinition = "TEXT")
    private String houseRules;

    /** Cancellation and booking policies */
    @Column(columnDefinition = "TEXT")
    private String policies;

    /** Nearby attractions and points of interest */
    @Column(columnDefinition = "TEXT")
    private String nearbyAttractions;

    /** Standard check-in time, e.g. "14:00" */
    @Column(length = 10)
    private String checkInTime = "14:00";

    /** Standard check-out time, e.g. "12:00" */
    @Column(length = 10)
    private String checkOutTime = "12:00";

    // Maps & Location
    private Double latitude;
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String nearbyRestaurants;

    @Column(columnDefinition = "TEXT")
    private String nearbyCafes;

    @Column(columnDefinition = "TEXT")
    private String nearbyAirport;

    @Column(columnDefinition = "TEXT")
    private String directions;

    // Booking Rules
    @Column(nullable = false)
    private Integer minimumStay = 1;

    @Column(nullable = false)
    private Integer maximumStay = 30;

    @ManyToMany(mappedBy = "rooms", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("rooms")
    private List<Promotion> promotions = new ArrayList<>();

    public Room() {
    }

    public Room(Integer id, String roomNumber, RoomTypeEntity roomType, BigDecimal pricePerNight,
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

    public RoomTypeEntity getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomTypeEntity roomType) {
        this.roomType = roomType;
    }

    public List<Amenity> getAmenities() {
        return amenities;
    }

    public List<Integer> getAmenityIds() {
        if (amenities == null)
            return new java.util.ArrayList<>();
        return amenities.stream().map(Amenity::getId).toList();
    }

    public void setAmenities(List<Amenity> amenities) {
        this.amenities = amenities;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public List<String> getGalleryImages() { return galleryImages; }
    public void setGalleryImages(List<String> galleryImages) { this.galleryImages = galleryImages; }
    public String getHouseRules() { return houseRules; }
    public void setHouseRules(String houseRules) { this.houseRules = houseRules; }
    public String getPolicies() { return policies; }
    public void setPolicies(String policies) { this.policies = policies; }
    public String getNearbyAttractions() { return nearbyAttractions; }
    public void setNearbyAttractions(String nearbyAttractions) { this.nearbyAttractions = nearbyAttractions; }
    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getNearbyRestaurants() { return nearbyRestaurants; }
    public void setNearbyRestaurants(String nearbyRestaurants) { this.nearbyRestaurants = nearbyRestaurants; }
    public String getNearbyCafes() { return nearbyCafes; }
    public void setNearbyCafes(String nearbyCafes) { this.nearbyCafes = nearbyCafes; }
    public String getNearbyAirport() { return nearbyAirport; }
    public void setNearbyAirport(String nearbyAirport) { this.nearbyAirport = nearbyAirport; }
    public String getDirections() { return directions; }
    public void setDirections(String directions) { this.directions = directions; }
    public Integer getMinimumStay() { return minimumStay; }
    public void setMinimumStay(Integer minimumStay) { this.minimumStay = minimumStay; }
    public Integer getMaximumStay() { return maximumStay; }
    public void setMaximumStay(Integer maximumStay) { this.maximumStay = maximumStay; }

    // Helper method
    public String getRoomTypeDisplayName() {
        return roomType != null ? roomType.getDisplayName() : "";
    }

    public List<Promotion> getPromotions() { return promotions; }
    public void setPromotions(List<Promotion> promotions) { this.promotions = promotions; }
}