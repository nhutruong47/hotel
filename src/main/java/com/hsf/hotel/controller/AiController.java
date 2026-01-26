package com.hsf.hotel.controller;

import com.hsf.hotel.model.ChatMessage;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.ChatMessageRepository;
import com.hsf.hotel.service.OllamaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class AiController {

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/ai-recommend")
    public String aiRecommendPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Load chat history for this user (last 20 messages, reversed to show oldest
        // first)
        List<ChatMessage> history = chatMessageRepository.findTop20ByUserOrderByCreatedAtDesc(user);
        Collections.reverse(history);
        model.addAttribute("chatHistory", history);

        return "ai-recommend";
    }

    @PostMapping("/ai-recommend")
    @ResponseBody
    public Map<String, String> getAiRecommendation(@RequestBody Map<String, String> request,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Map.of("error", "Vui lòng đăng nhập");
        }

        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Map.of("error", "Vui lòng nhập yêu cầu của bạn");
        }

        // Get AI response
        String aiResponse = ollamaService.getAiRecommendation(userMessage);

        // Save to database
        try {
            ChatMessage chatMessage = new ChatMessage(user, userMessage, aiResponse);
            chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            System.err.println("Failed to save chat message: " + e.getMessage());
        }

        return Map.of("response", aiResponse);
    }

    @PostMapping("/ai-recommend/clear")
    @ResponseBody
    public Map<String, String> clearHistory(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Map.of("error", "Vui lòng đăng nhập");
        }

        try {
            chatMessageRepository.deleteByUser(user);
            return Map.of("success", "Đã xóa lịch sử chat");
        } catch (Exception e) {
            return Map.of("error", "Không thể xóa lịch sử: " + e.getMessage());
        }
    }
}
