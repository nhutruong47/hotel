package com.hsf.hotel.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 64) String phone,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 5000) String message,
        @Size(max = 32) String type
) {
}
