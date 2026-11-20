package com.hsf.hotel.review.dto;

import com.hsf.hotel.review.model.Review;

import java.time.LocalDateTime;
import java.util.List;

/** Moderation view of a review (P0-04). Includes moderation fields, never the account internals. */
public record AdminReviewResponse(
        Integer id,
        Integer rating,
        String comment,
        List<String> photos,
        String adminReply,
        LocalDateTime adminReplyAt,
        Boolean isHidden,
        Integer reportCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserRef user,
        RoomRef room
) {
    public record UserRef(Integer id, String username, String email, String fullName) {}

    public record RoomRef(Integer id, String roomNumber) {}

    public static AdminReviewResponse from(Review r) {
        if (r == null) return null;
        return new AdminReviewResponse(
                r.getId(),
                r.getRating(),
                r.getComment(),
                r.getPhotos() != null ? List.copyOf(r.getPhotos()) : List.of(),
                r.getAdminReply(),
                r.getAdminReplyAt(),
                r.getIsHidden(),
                r.getReportCount(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getUser() == null ? null : new UserRef(r.getUser().getId(), r.getUser().getUsername(),
                        r.getUser().getEmail(), r.getUser().getFullName()),
                r.getRoom() == null ? null : new RoomRef(r.getRoom().getId(), r.getRoom().getRoomNumber())
        );
    }
}
