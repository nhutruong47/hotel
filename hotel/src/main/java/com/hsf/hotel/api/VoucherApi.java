package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherApi {

    private final VoucherService voucherService;

    public VoucherApi(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse> validate(@RequestParam String code) {
        VoucherService.VoucherValidationResult res = voucherService.validateVoucher(code);
        Map<String, Object> data = new HashMap<>();
        data.put("valid", res.valid);
        data.put("message", res.message);
        data.put("amount", res.amount != null ? res.amount.toString() : "0");
        data.put("percent", res.percent);
        if (res.voucher != null) {
            data.put("code", res.voucher.getCode());
            data.put("expiryDate", res.voucher.getExpiryDate() != null
                    ? res.voucher.getExpiryDate().toString() : null);
        }
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Calculates the discount for a given subtotal. Useful for the SPA to
     * preview the price impact before submitting a booking.
     */
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse> preview(@RequestParam String code,
                                              @RequestParam BigDecimal subtotal) {
        VoucherService.VoucherValidationResult res = voucherService.validateVoucher(code);
        Map<String, Object> data = new HashMap<>();
        data.put("valid", res.valid);
        data.put("message", res.message);
        if (!res.valid) {
            data.put("discount", "0");
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        BigDecimal discount;
        if (res.percent) {
            discount = subtotal.multiply(res.amount).divide(BigDecimal.valueOf(100));
        } else {
            discount = res.amount;
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        data.put("discount", discount.toString());
        data.put("finalPrice", subtotal.subtract(discount).toString());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}