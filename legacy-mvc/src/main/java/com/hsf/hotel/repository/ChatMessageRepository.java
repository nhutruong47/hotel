package com.hsf.hotel.repository;

import com.hsf.hotel.model.ChatMessage;
import com.hsf.hotel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    List<ChatMessage> findByUserOrderByCreatedAtAsc(User user);

    List<ChatMessage> findTop20ByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
