package com.hsf.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faq_items", indexes = {
    @Index(name = "idx_faq_category", columnList = "category"),
    @Index(name = "idx_faq_order", columnList = "displayOrder")
})
public class FaqItem {

    public enum FaqCategory {
        BOOKING("Đặt phòng"),
        PAYMENT("Thanh toán"),
        REFUND("Hoàn tiền"),
        CANCELLATION("Hủy phòng"),
        POLICIES("Chính sách"),
        SERVICES("Dịch vụ"),
        FACILITIES("Tiện ích"),
        TRANSPORT("Di chuyển"),
        GENERAL("Chung");

        private final String displayName;
        FaqCategory(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaqCategory category = FaqCategory.GENERAL;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean isPublished = true;

    private Integer helpfulCount = 0;
    private Integer notHelpfulCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private String metaKeywords;

    public FaqItem() {}

    public FaqItem(String question, String answer, FaqCategory category) {
        this.question = question;
        this.answer = answer;
        this.category = category;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public FaqCategory getCategory() { return category; }
    public void setCategory(FaqCategory category) { this.category = category; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }
    public Integer getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(Integer helpfulCount) { this.helpfulCount = helpfulCount; }
    public Integer getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(Integer notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getMetaKeywords() { return metaKeywords; }
    public void setMetaKeywords(String metaKeywords) { this.metaKeywords = metaKeywords; }

    public void incrementHelpful() {
        this.helpfulCount = (this.helpfulCount == null ? 0 : this.helpfulCount) + 1;
    }

    public void incrementNotHelpful() {
        this.notHelpfulCount = (this.notHelpfulCount == null ? 0 : this.notHelpfulCount) + 1;
    }
}
