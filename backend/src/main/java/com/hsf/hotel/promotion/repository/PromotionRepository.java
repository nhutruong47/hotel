package com.hsf.hotel.promotion.repository;

import com.hsf.hotel.promotion.model.Promotion;
import com.hsf.hotel.promotion.model.Promotion.PromotionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate <= :today AND p.endDate >= :today ORDER BY p.displayOrder ASC")
    List<Promotion> findActivePromotions(@Param("today") LocalDate today);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate <= :today AND p.endDate >= :today AND p.isFeatured = true ORDER BY p.displayOrder ASC")
    List<Promotion> findFeaturedPromotions(@Param("today") LocalDate today);

    List<Promotion> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<Promotion> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(PromotionCategory category);

    Optional<Promotion> findByPromoCode(String promoCode);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.promoCode = :promoCode")
    Optional<Promotion> findActiveByPromoCode(@Param("promoCode") String promoCode);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.promoCode IS NOT NULL AND " +
           "p.startDate <= :today AND p.endDate >= :today AND " +
           "(p.maximumUses IS NULL OR p.currentUses < p.maximumUses)")
    List<Promotion> findValidPromoCodes(@Param("today") LocalDate today);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.endDate < :today ORDER BY p.endDate DESC")
    List<Promotion> findExpiredPromotions(@Param("today") LocalDate today);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate > :today ORDER BY p.startDate ASC")
    List<Promotion> findUpcomingPromotions(@Param("today") LocalDate today);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.countdownEndDate IS NOT NULL AND p.countdownEndDate > :now")
    List<Promotion> findActiveWithCountdown(@Param("now") java.time.LocalDateTime now);

    long countByIsActiveTrue();
}
