package com.hsf.hotel.promotion.service;

import com.hsf.hotel.promotion.dto.PromotionResponse;
import com.hsf.hotel.promotion.dto.PromotionValidationResponse;
import com.hsf.hotel.promotion.model.Promotion;
import com.hsf.hotel.promotion.model.Promotion.PromotionCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read side for promotion APIs. Only immutable DTOs cross the service boundary. */
@Service
@RequiredArgsConstructor
public class PromotionQueryService {
    private final PromotionService promotionService;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "promotionViews", key = "'active'")
    public List<PromotionResponse> active() {
        return map(promotionService.getActivePromotions());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "promotionViews", key = "'featured'")
    public List<PromotionResponse> featured() {
        return map(promotionService.getFeaturedPromotions());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "promotionViews", key = "'category_' + #category.name()")
    public List<PromotionResponse> byCategory(PromotionCategory category) {
        return map(promotionService.getPromotionsByCategory(category));
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> expired() {
        return map(promotionService.getExpiredPromotions());
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> upcoming() {
        return map(promotionService.getUpcomingPromotions());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "promotionViews", key = "'countdown'")
    public List<PromotionResponse> countdown() {
        return map(promotionService.getPromotionsWithCountdown());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "promotionViews", key = "'grouped'")
    public Map<PromotionCategory, List<PromotionResponse>> grouped() {
        return active().stream().collect(Collectors.groupingBy(PromotionResponse::category));
    }

    @Transactional(readOnly = true)
    public PromotionValidationResponse validate(String code, BigDecimal amount, int nights) {
        try {
            Promotion promotion = promotionService.validatePromoCode(code, amount, nights);
            BigDecimal discount = promotionService.calculateDiscount(promotion, amount);
            return PromotionValidationResponse.valid(
                    code, discount, amount.subtract(discount), PromotionResponse.from(promotion));
        } catch (RuntimeException ex) {
            return PromotionValidationResponse.invalid(code, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PromotionValidationResponse preview(String code, BigDecimal subtotal) {
        try {
            Promotion promotion = promotionService.getAllPromotions().stream()
                    .filter(item -> code.equalsIgnoreCase(item.getPromoCode()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Promo code not found"));
            BigDecimal discount = promotionService.calculateDiscount(promotion, subtotal);
            return PromotionValidationResponse.valid(
                    code, discount, subtotal.subtract(discount), PromotionResponse.from(promotion));
        } catch (RuntimeException ex) {
            return PromotionValidationResponse.invalid(code, ex.getMessage());
        }
    }

    private static List<PromotionResponse> map(List<Promotion> promotions) {
        return promotions.stream().map(PromotionResponse::from).toList();
    }
}
