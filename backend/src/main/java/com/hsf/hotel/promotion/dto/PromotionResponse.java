package com.hsf.hotel.promotion.dto;

import com.hsf.hotel.promotion.model.Promotion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Public, cache-safe representation of a promotion. */
public record PromotionResponse(
        Integer id,
        String title,
        String subtitle,
        String description,
        String termsConditions,
        String imageUrl,
        String bannerUrl,
        Promotion.PromotionCategory category,
        Boolean isActive,
        Boolean isFeatured,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime countdownEndDate,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal minimumBookingAmount,
        Integer minimumNights,
        Integer maximumUses,
        Integer currentUses,
        String promoCode,
        Integer displayOrder,
        List<RoomReference> rooms
) implements Serializable {
    public PromotionResponse {
        rooms = rooms == null ? List.of() : List.copyOf(rooms);
    }

    public static PromotionResponse from(Promotion promotion) {
        List<RoomReference> roomReferences = promotion.getRooms() == null
                ? List.of()
                : promotion.getRooms().stream()
                        .map(room -> new RoomReference(room.getId(), room.getRoomNumber()))
                        .toList();
        return new PromotionResponse(
                promotion.getId(), promotion.getTitle(), promotion.getSubtitle(),
                promotion.getDescription(), promotion.getTermsConditions(),
                promotion.getImageUrl(), promotion.getBannerUrl(), promotion.getCategory(),
                promotion.getIsActive(), promotion.getIsFeatured(), promotion.getStartDate(),
                promotion.getEndDate(), promotion.getCountdownEndDate(), promotion.getDiscountPercent(),
                promotion.getDiscountAmount(), promotion.getMinimumBookingAmount(),
                promotion.getMinimumNights(), promotion.getMaximumUses(), promotion.getCurrentUses(),
                promotion.getPromoCode(), promotion.getDisplayOrder(), roomReferences
        );
    }

    public record RoomReference(Integer id, String roomNumber) implements Serializable {
    }
}
