package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.model.ChatMessage;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.ChatMessageRepository;
import com.hsf.hotel.service.GeminiService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiApi {

    private static final Logger log = LoggerFactory.getLogger(AiApi.class);
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final GeminiService geminiService;
    private final ChatMessageRepository chatMessageRepository;

    public AiApi(GeminiService geminiService, ChatMessageRepository chatMessageRepository) {
        this.geminiService = geminiService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse> history(HttpSession session) {
        User user = requireUser(session);
        List<ChatMessage> history = chatMessageRepository.findTop20ByUserOrderByCreatedAtDesc(user);
        Collections.reverse(history);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse> recommend(@RequestBody Map<String, String> request, HttpSession session) {
        User user = requireUser(session);
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCodes.BAD_REQUEST, "Vui lòng nhập yêu cầu"));
        }
        message = message.trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCodes.BAD_REQUEST, "Message is too long"));
        }
        String ai = geminiService.getAiRecommendation(message);
        try {
            chatMessageRepository.save(new ChatMessage(user, message, ai));
        } catch (Exception e) {
            log.warn("Failed to persist chat message for user {}: {}", user.getUsername(), e.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("response", ai);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse> clear(HttpSession session) {
        User user = requireUser(session);
        chatMessageRepository.deleteByUser(user);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã xóa lịch sử chat")));
    }

    private static User requireUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, com.hsf.hotel.config.ErrorCodes.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        return user;
    }
}
