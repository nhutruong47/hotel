package com.hsf.hotel.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.notification.dto.NotificationResponse;
import com.hsf.hotel.notification.model.Notification;
import com.hsf.hotel.review.dto.PublicReviewResponse;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.wishlist.dto.WishlistItemResponse;
import com.hsf.hotel.wishlist.model.WishlistItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfServiceResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void wishlistResponseDoesNotSerializeOwner() throws Exception {
        User owner = ownerWithSensitiveFields();
        RoomTypeEntity type = new RoomTypeEntity("Pool Villa", "Private pool");
        Room room = new Room();
        room.setId(7);
        room.setRoomNumber("A-07");
        room.setRoomType(type);
        room.setPricePerNight(BigDecimal.valueOf(2_000_000));

        WishlistItem item = new WishlistItem(owner, room);
        item.setId(11);

        String json = objectMapper.writeValueAsString(WishlistItemResponse.from(item));

        assertTrue(json.contains("\"roomNumber\":\"A-07\""));
        assertFalse(json.contains("\"user\""));
        assertFalse(json.contains("private@example.test"));
        assertFalse(json.contains("secret-hash"));
    }

    @Test
    void notificationResponseDoesNotSerializeOwner() throws Exception {
        Notification notification = new Notification(
                ownerWithSensitiveFields(), "Payment", "Payment received", "PAYMENT");
        notification.setId(12);

        String json = objectMapper.writeValueAsString(NotificationResponse.from(notification));

        assertTrue(json.contains("\"title\":\"Payment\""));
        assertFalse(json.contains("\"user\""));
        assertFalse(json.contains("private@example.test"));
        assertFalse(json.contains("secret-hash"));
    }

    @Test
    void publicReviewResponseDoesNotSerializeAccountOrModerationFields() throws Exception {
        User owner = ownerWithSensitiveFields();
        owner.setFullName("Guest Name");
        Room room = new Room();
        room.setId(7);
        room.setRoomNumber("A-07");

        Review review = new Review();
        review.setId(13);
        review.setUser(owner);
        review.setRoom(room);
        review.setRating(5);
        review.setComment("Excellent stay");
        review.setReportCount(8);
        review.setIsHidden(false);

        String json = objectMapper.writeValueAsString(PublicReviewResponse.from(review));

        assertTrue(json.contains("\"fullName\":\"Guest Name\""));
        assertTrue(json.contains("\"roomNumber\":\"A-07\""));
        assertFalse(json.contains("private@example.test"));
        assertFalse(json.contains("secret-hash"));
        assertFalse(json.contains("reportCount"));
        assertFalse(json.contains("isHidden"));
        assertFalse(json.contains("booking"));
    }

    private static User ownerWithSensitiveFields() {
        User user = new User();
        user.setId(99);
        user.setUsername("private-user");
        user.setEmail("private@example.test");
        user.setPasswordHash("secret-hash");
        return user;
    }
}
