package com.hsf.hotel.faq.dto;

import com.hsf.hotel.faq.model.FaqItem;

import java.io.Serializable;

/** Public FAQ projection; persistence internals never reach the wire. */
public record FaqResponse(
        Integer id,
        String question,
        String answer,
        FaqItem.FaqCategory category,
        Integer displayOrder,
        Boolean isPublished,
        Integer helpfulCount,
        Integer notHelpfulCount
) implements Serializable {
    public static FaqResponse from(FaqItem item) {
        return new FaqResponse(
                item.getId(), item.getQuestion(), item.getAnswer(), item.getCategory(),
                item.getDisplayOrder(), item.getIsPublished(), item.getHelpfulCount(),
                item.getNotHelpfulCount()
        );
    }
}
