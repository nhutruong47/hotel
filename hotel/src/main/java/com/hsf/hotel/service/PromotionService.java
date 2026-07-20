package com.hsf.hotel.service;

import com.hsf.hotel.model.Promotion;
import com.hsf.hotel.model.Promotion.PromotionCategory;
import com.hsf.hotel.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import com.hsf.hotel.exception.VoucherException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    @Cacheable(cacheNames = "promotions", key = "'active'")
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDate.now());
    }

    @Cacheable(cacheNames = "promotions", key = "'featured'")
    public List<Promotion> getFeaturedPromotions() {
        return promotionRepository.findFeaturedPromotions(LocalDate.now());
    }

    @Cacheable(cacheNames = "promotions", key = "'category_' + #category.name()")
    public List<Promotion> getPromotionsByCategory(PromotionCategory category) {
        return promotionRepository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);
    }

    public List<Promotion> getExpiredPromotions() {
        return promotionRepository.findExpiredPromotions(LocalDate.now());
    }

    public List<Promotion> getUpcomingPromotions() {
        return promotionRepository.findUpcomingPromotions(LocalDate.now());
    }

    @Cacheable(cacheNames = "promotions", key = "'countdown'")
    public List<Promotion> getPromotionsWithCountdown() {
        return promotionRepository.findActiveWithCountdown(LocalDateTime.now());
    }

    @Cacheable(cacheNames = "promotions", key = "'all'")
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Cacheable(cacheNames = "promotions", key = "'grouped'")
    public Map<PromotionCategory, List<Promotion>> getPromotionsGroupedByCategory() {
        List<Promotion> promotions = getActivePromotions();
        return promotions.stream()
                .collect(Collectors.groupingBy(Promotion::getCategory));
    }

    public Promotion validatePromoCode(String code, BigDecimal bookingAmount, int nights) {
        Promotion promo = promotionRepository.findByPromoCode(code)
                .orElseThrow(() -> new RuntimeException("Promo code not found"));

        LocalDate today = LocalDate.now();
        if (!promo.getIsActive()) {
            throw new VoucherException("Promo code is no longer active");
        }
        if (today.isBefore(promo.getStartDate())) {
            throw new VoucherException("Promo code is not yet available");
        }
        if (today.isAfter(promo.getEndDate())) {
            throw new VoucherException("Promo code has expired");
        }
        if (promo.getMaximumUses() != null && promo.getCurrentUses() >= promo.getMaximumUses()) {
            throw new VoucherException("Promo code has reached its usage limit");
        }
        if (bookingAmount.compareTo(promo.getMinimumBookingAmount()) < 0) {
            throw new VoucherException("Minimum booking amount of " + promo.getMinimumBookingAmount() + " required");
        }
        if (nights < promo.getMinimumNights()) {
            throw new VoucherException("Minimum " + promo.getMinimumNights() + " nights required");
        }

        return promo;
    }

    public BigDecimal calculateDiscount(Promotion promo, BigDecimal bookingAmount) {
        BigDecimal discount = BigDecimal.ZERO;
        if (promo.getDiscountPercent() != null) {
            discount = bookingAmount.multiply(promo.getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.FLOOR);
        } else if (promo.getDiscountAmount() != null) {
            discount = promo.getDiscountAmount();
        }
        return discount.min(bookingAmount);
    }

    @Transactional
    public Promotion incrementUsage(Integer id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        promo.setCurrentUses(promo.getCurrentUses() + 1);
        return promotionRepository.save(promo);
    }

    @Transactional
    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public Promotion createPromotion(Promotion promotion) {
        promotion.setCreatedAt(LocalDateTime.now());
        return promotionRepository.save(promotion);
    }

    @Transactional
    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public Promotion updatePromotion(Integer id, Promotion updated) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        existing.setTitle(updated.getTitle());
        existing.setSubtitle(updated.getSubtitle());
        existing.setDescription(updated.getDescription());
        existing.setTermsConditions(updated.getTermsConditions());
        existing.setImageUrl(updated.getImageUrl());
        existing.setBannerUrl(updated.getBannerUrl());
        existing.setCategory(updated.getCategory());
        existing.setIsActive(updated.getIsActive());
        existing.setIsFeatured(updated.getIsFeatured());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setCountdownEndDate(updated.getCountdownEndDate());
        existing.setDiscountPercent(updated.getDiscountPercent());
        existing.setDiscountAmount(updated.getDiscountAmount());
        existing.setMinimumBookingAmount(updated.getMinimumBookingAmount());
        existing.setMinimumNights(updated.getMinimumNights());
        existing.setMaximumUses(updated.getMaximumUses());
        existing.setPromoCode(updated.getPromoCode());
        existing.setDisplayOrder(updated.getDisplayOrder());
        existing.setUpdatedAt(LocalDateTime.now());
        return promotionRepository.save(existing);
    }

    @CacheEvict(cacheNames = "promotions", allEntries = true)
    public void deletePromotion(Integer id) {
        promotionRepository.deleteById(id);
    }
}
