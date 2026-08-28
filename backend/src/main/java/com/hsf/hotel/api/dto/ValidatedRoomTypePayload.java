package com.hsf.hotel.api.dto;

import jakarta.validation.constraints.*;

/**
 * Validated DTO for creating/updating room types.
 */
public class ValidatedRoomTypePayload {

    public Integer id;

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(min = 2, max = 50, message = "Tên loại phòng phải từ 2-50 ký tự")
    @Pattern(regexp = "^[A-Z][A-Za-z0-9\\s]*$", 
             message = "Tên loại phòng phải bắt đầu bằng chữ in hoa")
    public String name;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    public String description;
}
