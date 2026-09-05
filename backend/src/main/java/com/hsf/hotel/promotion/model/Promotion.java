package com.hsf.hotel.promotion.model;
import com.hsf.hotel.room.model.Room;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promotion_active", columnList = "isActive"),
    @Index(name = "idx_promotion_start", columnList = "startDate"),
    @Index(name = "idx_promotion_end", columnList = "endDate"),
    @Index(name = "idx_promotion_category", columnList = "category")
})
public class Promotion implements java.io.Serializable {

    public enum PromotionCategory {
        SEASONAL("Mùa cao điểm"),
        WEEKEND("Cuối tuần"),
        HOLIDAY("Ngày lễ"),
        HONEYMOON("Tuần trăng mật"),
        FAMILY("Gia đình"),
        EARLY_BIRD("Đặt sớm"),
        LAST_MINUTE("Last Minute"),
        LOYALTY("Khách quen"),
        LONG_STAY("Dài ngày"),
        SPECIAL("Đặc biệt");

        private final String displayName;
        PromotionCategory(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String subtitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String termsConditions;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionCategory category = PromotionCategory.SEASONAL;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isFeatured = false;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDateTime countdownEndDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountPercent;

    @Column(precision = 12, scale = 0)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private BigDecimal minimumBookingAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer minimumNights = 1;

    private Integer maximumUses;

    private Integer currentUses = 0;

    @Column(length = 100)
    private String promoCode;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "promotion_rooms",
        joinColumns = @JoinColumn(name = "promotion_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    @OrderBy("id ASC")
    private List<Room> rooms = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public Promotion() {}

    public boolean isCurrentlyActive() {
        if (!isActive) return false;
        LocalDate now = LocalDate.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    public long getDaysRemaining() {
        if (countdownEndDate == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(countdownEndDate)) return 0;
        return java.time.Duration.between(now, countdownEndDate).toDays();
    }

    public long getHoursRemaining() {
        if (countdownEndDate == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(countdownEndDate)) return 0;
        return java.time.Duration.between(now, countdownEndDate).toHours() % 24;
    }

    public long getMinutesRemaining() {
        if (countdownEndDate == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(countdownEndDate)) return 0;
        return java.time.Duration.between(now, countdownEndDate).toMinutes() % 60;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTermsConditions() { return termsConditions; }
    public void setTermsConditions(String termsConditions) { this.termsConditions = termsConditions; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public PromotionCategory getCategory() { return category; }
    public void setCategory(PromotionCategory category) { this.category = category; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDateTime getCountdownEndDate() { return countdownEndDate; }
    public void setCountdownEndDate(LocalDateTime countdownEndDate) { this.countdownEndDate = countdownEndDate; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getMinimumBookingAmount() { return minimumBookingAmount; }
    public void setMinimumBookingAmount(BigDecimal minimumBookingAmount) { this.minimumBookingAmount = minimumBookingAmount; }
    public Integer getMinimumNights() { return minimumNights; }
    public void setMinimumNights(Integer minimumNights) { this.minimumNights = minimumNights; }
    public Integer getMaximumUses() { return maximumUses; }
    public void setMaximumUses(Integer maximumUses) { this.maximumUses = maximumUses; }
    public Integer getCurrentUses() { return currentUses; }
    public void setCurrentUses(Integer currentUses) { this.currentUses = currentUses; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }
}

