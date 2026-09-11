package com.hsf.hotel.wishlist.repository;

import com.hsf.hotel.wishlist.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    @EntityGraph(attributePaths = {"room", "room.roomType"})
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Integer userId);
    Optional<WishlistItem> findByUserIdAndRoomId(Integer userId, Integer roomId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.user.id = :userId AND w.room.id = :roomId")
    int deleteByUserIdAndRoomId(@Param("userId") Integer userId, @Param("roomId") Integer roomId);
}
