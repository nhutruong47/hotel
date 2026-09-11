package com.hsf.hotel.review.repository;

import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewVisibilityIntegrationTest {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @Test
    void publicQueriesExcludeHiddenReviewsFromListAndAggregates() {
        User user = new User();
        user.setUsername("review-visibility-user");
        user.setEmail("review-visibility@example.test");
        user.setPasswordHash("encoded-test-password");
        user.setEmailVerified(true);
        user = userRepository.save(user);

        RoomTypeEntity type = roomTypeRepository.save(
                new RoomTypeEntity("Visibility Suite", "Visibility Suite"));
        Room room = new Room();
        room.setRoomNumber("VIS-001");
        room.setRoomType(type);
        room.setPricePerNight(BigDecimal.valueOf(1_000_000));
        room = roomRepository.save(room);

        Review visible = review(user, room, 5, false);
        Review hidden = review(user, room, 1, true);
        reviewRepository.saveAllAndFlush(List.of(visible, hidden));

        List<Review> publicReviews = reviewRepository.findByRoomOrderByCreatedAtDesc(room);

        assertEquals(1, publicReviews.size());
        assertEquals(visible.getId(), publicReviews.get(0).getId());
        assertEquals(1, reviewRepository.countByRoom(room));
        assertEquals(5.0, reviewRepository.getAverageRatingByRoom(room));
    }

    private static Review review(User user, Room room, int rating, boolean hidden) {
        Review review = new Review();
        review.setUser(user);
        review.setRoom(room);
        review.setRating(rating);
        review.setIsHidden(hidden);
        return review;
    }
}
