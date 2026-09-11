package com.hsf.hotel.review.dto;

import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Deliberately small public projection. Moderation flags, booking details and
 * account fields never cross the public review boundary.
 */
public record PublicReviewResponse(
        Integer id,
        Integer rating,
        Integer ratingCleanliness,
        Integer ratingService,
        Integer ratingLocation,
        Integer ratingValue,
        Integer ratingAmenities,
        String comment,
        List<String> photos,
        String adminReply,
        LocalDateTime adminReplyAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Author user,
        RoomReference room
) {
    public PublicReviewResponse {
        photos = photos == null ? List.of() : List.copyOf(photos);
    }

    public static PublicReviewResponse from(Review review) {
        return new PublicReviewResponse(
                review.getId(),
                review.getRating(),
                review.getRatingCleanliness(),
                review.getRatingService(),
                review.getRatingLocation(),
                review.getRatingValue(),
                review.getRatingAmenities(),
                review.getComment(),
                review.getPhotos(),
                review.getAdminReply(),
                review.getAdminReplyAt(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                Author.from(review.getUser()),
                RoomReference.from(review.getRoom())
        );
    }

    public record Author(String fullName, String avatarFilename) {
        private static Author from(User user) {
            if (user == null) {
                return null;
            }
            String displayName = user.getFullName();
            if (displayName == null || displayName.isBlank()) {
                displayName = user.getUsername();
            }
            return new Author(displayName, user.getAvatarFilename());
        }
    }

    public record RoomReference(Integer id, String roomNumber, String slug) {
        private static RoomReference from(Room room) {
            return room == null ? null : new RoomReference(room.getId(), room.getRoomNumber(), room.getSlug());
        }
    }
}
