package com.hsf.hotel.ai.repository;

import com.hsf.hotel.ai.model.ChatMessage;
import com.hsf.hotel.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    List<ChatMessage> findByUserOrderByCreatedAtAsc(User user);

    List<ChatMessage> findTop20ByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
