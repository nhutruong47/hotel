package com.hsf.hotel.api;

import com.hsf.hotel.model.Promotion;
import com.hsf.hotel.model.Promotion.PromotionCategory;
import com.hsf.hotel.service.PromotionService;
import com.hsf.hotel.exception.VoucherException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionApi {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        return ResponseEntity.ok(promotionService.getActivePromotions());
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Promotion>> getFeaturedPromotions() {
        return ResponseEntity.ok(promotionService.getFeaturedPromotions());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Promotion>> getByCategory(@PathVariable PromotionCategory category) {
        return ResponseEntity.ok(promotionService.getPromotionsByCategory(category));
    }

    @GetMapping("/expired")
    public ResponseEntity<List<Promotion>> getExpiredPromotions() {
        return ResponseEntity.ok(promotionService.getExpiredPromotions());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Promotion>> getUpcomingPromotions() {
        return ResponseEntity.ok(promotionService.getUpcomingPromotions());
    }

    @GetMapping("/countdown")
    public ResponseEntity<List<Promotion>> getPromotionsWithCountdown() {
        return ResponseEntity.ok(promotionService.getPromotionsWithCountdown());
    }

    @GetMapping("/grouped")
    public ResponseEntity<Map<PromotionCategory, List<Promotion>>> getGroupedPromotions() {
        return ResponseEntity.ok(promotionService.getPromotionsGroupedByCategory());
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePromoCode(
            @RequestParam String code,
            @RequestParam BigDecimal amount,
            @RequestParam int nights) {
        try {
            Promotion promo = promotionService.validatePromoCode(code, amount, nights);
            BigDecimal discount = promotionService.calculateDiscount(promo, amount);
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "promotion", promo,
                "discount", discount,
                "finalAmount", amount.subtract(discount)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewDiscount(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {
        try {
            List<Promotion> allPromos = promotionService.getAllPromotions();
            Promotion promo = allPromos.stream()
                    .filter(p -> code.equalsIgnoreCase(p.getPromoCode()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Promo code not found"));

            BigDecimal discount = promotionService.calculateDiscount(promo, subtotal);
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "code", code,
                "discountType", promo.getDiscountPercent() != null ? "percent" : "fixed",
                "discountValue", promo.getDiscountPercent() != null ? promo.getDiscountPercent() : promo.getDiscountAmount(),
                "discount", discount,
                "finalAmount", subtotal.subtract(discount),
                "promotion", promo
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", e.getMessage()
            ));
        }
    }
}
