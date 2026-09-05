package com.hsf.hotel.wishlist.repository;

import com.hsf.hotel.wishlist.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserId(Integer userId);
    Optional<WishlistItem> findByUserIdAndRoomId(Integer userId, Integer roomId);
}
