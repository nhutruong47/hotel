package com.hsf.hotel.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Validated DTO for creating/updating rooms.
 */
public class ValidatedRoomPayload {

    public Integer id;

    @NotBlank(message = "Số phòng không được để trống")
    @Size(min = 2, max = 20, message = "Số phòng phải từ 2-20 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Số phòng chỉ chứa chữ, số và dấu gạch ngang")
    public String roomNumber;

    public String roomType;
    public Integer roomTypeId;

    @NotNull(message = "Giá phòng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phòng phải lớn hơn 0")
    @DecimalMax(value = "999999999.99", message = "Giá phòng quá lớn")
    public BigDecimal pricePerNight;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    public String description;

    @Size(max = 500, message = "URL hình ảnh quá dài")
    @Pattern(regexp = "^(https?://|/images/|/uploads/).*$", 
             flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "URL hình ảnh không hợp lệ")
    public String imageUrl;

    public Boolean isAvailable;

    @Size(max = 50, message = "Danh sách tiện ích quá dài")
    public List<@Min(value = 1, message = "ID tiện ích không hợp lệ") Integer> amenityIds;

    @Min(value = 1, message = "Sức chứa tối thiểu là 1")
    @Max(value = 50, message = "Sức chứa tối đa là 50")
    public Integer capacity;

    @Min(value = 0, message = "Số phòng ngủ không thể âm")
    @Max(value = 20, message = "Số phòng ngủ quá nhiều")
    public Integer bedrooms;

    @DecimalMin(value = "-90.0", message = "Latitude không hợp lệ")
    @DecimalMax(value = "90.0", message = "Latitude không hợp lệ")
    public Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude không hợp lệ")
    @DecimalMax(value = "180.0", message = "Longitude không hợp lệ")
    public Double longitude;

    @Size(max = 500, message = "Thông tin nhà hàng gần đó quá dài")
    public String nearbyRestaurants;

    @Size(max = 500, message = "Thông tin quán cà phê quá dài")
    public String nearbyCafes;

    @Size(max = 500, message = "Thông tin sân bay quá dài")
    public String nearbyAirport;

    @Size(max = 1000, message = "Hướng dẫn đường quá dài")
    public String directions;

    @Min(value = 1, message = "Số đêm tối thiểu phải từ 1")
    @Max(value = 365, message = "Số đêm tối thiểu quá lớn")
    public Integer minimumStay;

    @Min(value = 1, message = "Số đêm tối đa phải từ 1")
    @Max(value = 365, message = "Số đêm tối đa quá lớn")
    public Integer maximumStay;
}
