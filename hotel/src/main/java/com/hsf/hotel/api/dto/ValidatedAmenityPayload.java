package com.hsf.hotel.api.dto;

import jakarta.validation.constraints.*;

/**
 * Validated DTO for creating/updating amenities.
 */
public class ValidatedAmenityPayload {

    public Integer id;

    @NotBlank(message = "Tên tiện ích không được để trống")
    @Size(min = 2, max = 100, message = "Tên tiện ích phải từ 2-100 ký tự")
    public String name;

    @Size(max = 50, message = "Icon code quá dài")
    @Pattern(regexp = "^fa-[a-z\\-]+$", message = "Icon code phải theo format FontAwesome (fa-xxx)")
    public String iconCode;
}
