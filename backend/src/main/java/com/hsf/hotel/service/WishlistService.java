package com.hsf.hotel.service;

import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.model.WishlistItem;
import com.hsf.hotel.repository.RoomRepository;
import com.hsf.hotel.repository.UserRepository;
import com.hsf.hotel.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

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

    public List<WishlistItem> getUserWishlist(Integer userId) {
        return wishlistRepository.findByUserId(userId);
    }

    /**
     * Toggle a wishlist entry for a user/room pair. Returns true if added,
     * false if removed.
     */
    @Transactional
    public boolean toggleWishlist(Integer userId, Integer roomId) {
        Optional<WishlistItem> existing = wishlistRepository.findByUserIdAndRoomId(userId, roomId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setRoom(room);
        try {
            wishlistRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException ex) {
            return wishlistRepository.findByUserIdAndRoomId(userId, roomId).isPresent();
        }
        return true;
    }
}
