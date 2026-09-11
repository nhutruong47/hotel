package com.hsf.hotel.wishlist.service;

import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.wishlist.model.WishlistItem;
import com.hsf.hotel.wishlist.dto.WishlistItemResponse;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.user.repository.UserRepository;
import com.hsf.hotel.wishlist.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           UserRepository userRepository,
                           RoomRepository roomRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getUserWishlist(Integer userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WishlistItemResponse::from)
                .toList();
    }

    /**
     * Toggle a wishlist entry for a user/room pair. Returns true if added,
     * false if removed.
     */
    @Transactional
    public boolean toggleWishlist(Integer userId, Integer roomId) {
        User user = lockUser(userId);
        Optional<WishlistItem> existing = wishlistRepository.findByUserIdAndRoomId(userId, roomId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false;
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setRoom(room);
        wishlistRepository.save(item);
        return true;
    }

    /** Add is idempotent: repeated requests leave the item present. */
    @Transactional
    public boolean addToWishlist(Integer userId, Integer roomId) {
        User user = lockUser(userId);
        if (wishlistRepository.findByUserIdAndRoomId(userId, roomId).isPresent()) {
            return false;
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        wishlistRepository.save(new WishlistItem(user, room));
        return true;
    }

    /** Remove is idempotent: repeated requests leave the item absent. */
    @Transactional
    public boolean removeFromWishlist(Integer userId, Integer roomId) {
        lockUser(userId);
        return wishlistRepository.deleteByUserIdAndRoomId(userId, roomId) > 0;
    }

    private User lockUser(Integer userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
