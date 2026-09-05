package com.hsf.hotel.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nested DTO container for password change / role change operations.
 * Static inner classes keep the JSON contract independent of each other
 * and make field-level validation easier to apply at the controller layer.
 */
public final class PasswordDTO {

    private PasswordDTO() {}

    /** Payload for changing the current user's password. */
    public static class UpdatePassword {

        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        private String currentPassword;

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "Mật khẩu phải có cả chữ và số")
        private String newPassword;

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
        @Size(min = 8, message = "Mật khẩu nhập lại phải có ít nhất 8 ký tự")
        private String confirmPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

        @JsonIgnore
        public boolean isConfirmed() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }
    }

    /** Payload for changing a user's role (admin only). */
    public static class UpdateRole {

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "USER|ADMIN", message = "Role must be USER or ADMIN")
        private String role;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}