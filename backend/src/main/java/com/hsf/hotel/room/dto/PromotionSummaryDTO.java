package com.hsf.hotel.room.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PromotionSummaryDTO implements Serializable {
    private Integer id;
    private String title;
    private String promoCode;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
}
