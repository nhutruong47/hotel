package com.hsf.hotel.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validated DTO for creating/updating vouchers.
 */
public class ValidatedVoucherPayload {

    public Integer id;

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 20, message = "Mã voucher phải từ 3-20 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã voucher chỉ chứa chữ in hoa và số")
    public String code;

    @NotNull(message = "Giá trị voucher không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị voucher phải lớn hơn 0")
    @DecimalMax(value = "999999999.99", message = "Giá trị voucher quá lớn")
    public BigDecimal amount;

    public String expiryDate;

    @Min(value = 1, message = "Số lượng phải từ 1")
    @Max(value = 999999, message = "Số lượng quá lớn")
    public Integer quantity;

    public Boolean percent;

    public boolean isValidDateFormat() {
        if (expiryDate == null || expiryDate.isBlank()) {
            return true; // null date is valid (no expiry)
        }
        try {
            LocalDate.parse(expiryDate);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
