package com.hsf.hotel.promotion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromotionValidationResponse(
        boolean valid,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal discount,
        BigDecimal finalAmount,
        PromotionResponse promotion,
        String error
) {
    public static PromotionValidationResponse valid(
            String code, BigDecimal discount, BigDecimal finalAmount, PromotionResponse promotion) {
        boolean percentage = promotion.discountPercent() != null;
        return new PromotionValidationResponse(
                true,
                code,
                percentage ? "percent" : "fixed",
                percentage ? promotion.discountPercent() : promotion.discountAmount(),
                discount,
                finalAmount,
                promotion,
                null
        );
    }

    public static PromotionValidationResponse invalid(String code, String error) {
        return new PromotionValidationResponse(false, code, null, null, null, null, null, error);
    }
}
