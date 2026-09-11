package com.hsf.hotel.wishlist.service;

import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import com.hsf.hotel.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WishlistConcurrencyIntegrationTest {

    @Autowired private WishlistService wishlistService;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @Test
    void concurrentAddsAreSerializedAndRemainIdempotent() throws Exception {
        long suffix = System.nanoTime();
        User user = createUser("wishlist_" + suffix);
        Room room = createRoom("WISH-" + suffix);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> add(user.getId(), room.getId(), ready, start));
            Future<Boolean> second = executor.submit(() -> add(user.getId(), room.getId(), ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int changed = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, changed);
            assertTrue(wishlistRepository.findByUserIdAndRoomId(user.getId(), room.getId()).isPresent());
        } finally {
            executor.shutdownNow();
        }

        assertTrue(wishlistService.removeFromWishlist(user.getId(), room.getId()));
        assertFalse(wishlistService.removeFromWishlist(user.getId(), room.getId()));
    }

    private boolean add(Integer userId, Integer roomId, CountDownLatch ready,
                        CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        return wishlistService.addToWishlist(userId, roomId);
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash("$2a$10$dummy");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Room createRoom(String number) {
        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("Wishlist type " + number);
        type.setDescription("Wishlist concurrency test");
        type = roomTypeRepository.save(type);

        Room room = new Room();
        room.setRoomNumber(number);
        room.setRoomType(type);
        room.setPricePerNight(BigDecimal.valueOf(1_000_000));
        room.setCapacity(2);
        room.setBedrooms(1);
        room.setIsAvailable(true);
        return roomRepository.save(room);
    }
}
