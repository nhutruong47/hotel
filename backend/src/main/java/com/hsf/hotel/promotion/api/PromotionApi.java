package com.hsf.hotel.promotion.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ApiResponses;
import com.hsf.hotel.promotion.dto.PromotionResponse;
import com.hsf.hotel.promotion.dto.PromotionValidationResponse;
import com.hsf.hotel.promotion.model.Promotion.PromotionCategory;
import com.hsf.hotel.promotion.service.PromotionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/promotions")
@RequiredArgsConstructor
public class PromotionApi {

    private final PromotionQueryService promotionQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActivePromotions() {
        return ApiResponses.ok(promotionQueryService.active());
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getFeaturedPromotions() {
        return ApiResponses.ok(promotionQueryService.featured());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getByCategory(@PathVariable PromotionCategory category) {
        return ApiResponses.ok(promotionQueryService.byCategory(category));
    }

    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getExpiredPromotions() {
        return ApiResponses.ok(promotionQueryService.expired());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getUpcomingPromotions() {
        return ApiResponses.ok(promotionQueryService.upcoming());
    }

    @GetMapping("/countdown")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getPromotionsWithCountdown() {
        return ApiResponses.ok(promotionQueryService.countdown());
    }

    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<Map<PromotionCategory, List<PromotionResponse>>>> getGroupedPromotions() {
        return ApiResponses.ok(promotionQueryService.grouped());
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionValidationResponse>> validatePromoCode(
            @RequestParam String code,
            @RequestParam BigDecimal amount,
            @RequestParam int nights) {
        return ApiResponses.ok(promotionQueryService.validate(code, amount, nights));
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<PromotionValidationResponse>> previewDiscount(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {
        return ApiResponses.ok(promotionQueryService.preview(code, subtotal));
    }
}
