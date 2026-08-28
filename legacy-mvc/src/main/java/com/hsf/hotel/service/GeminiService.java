package com.hsf.hotel.service;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

        @Value("${gemini.api.key:}")
        private String apiKey;

        @Value("${gemini.api.model:gemini-2.0-flash}")
        private String model;

        private final WebClient webClient;

        @Autowired
        private RoomRepository roomRepository;

        public GeminiService() {
                this.webClient = WebClient.builder()
                                .baseUrl("https://generativelanguage.googleapis.com")
                                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                                .build();
        }

        public String getAiRecommendation(String userRequest) {
                if (apiKey == null || apiKey.isBlank()) {
                        List<Room> availableRooms = roomRepository.findByIsAvailableTrue();
                        if (availableRooms.isEmpty()) {
                                return "AI assistant is not configured, and no rooms are currently available.";
                        }
                        return getFallbackRecommendation(availableRooms, userRequest);
                }

                System.out.println("🤖 AI Request: " + userRequest);

                // Get all available rooms to provide context to AI
                List<Room> availableRooms = roomRepository.findByIsAvailableTrue();
                System.out.println("📋 Found " + availableRooms.size() + " available rooms");

                if (availableRooms.isEmpty()) {
                        return "Hiện tại không có phòng trống. Vui lòng quay lại sau!";
                }

                String roomsContext = availableRooms.stream()
                                .map(room -> String.format(
                                                "- Phòng %s (%s): %s - Giá: %s VNĐ/đêm",
                                                room.getRoomNumber(),
                                                room.getRoomType().getDisplayName(),
                                                room.getDescription() != null ? room.getDescription() : "",
                                                room.getPricePerNight().toString()))
                                .collect(Collectors.joining("\n"));

                String prompt = String.format("""
                                Bạn là trợ lý AI của khách sạn Như Hotel. Giúp khách chọn phòng phù hợp.

                                PHÒNG TRỐNG:
                                %s

                                KHÁCH CẦN: %s

                                Gợi ý 1-2 phòng phù hợp nhất, giải thích ngắn gọn. Trả lời tiếng Việt.
                                """, roomsContext, userRequest);

                // Retry up to 3 times with backoff for rate limit errors
                int maxRetries = 3;
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        try {
                                System.out.println("🚀 Calling Gemini API (model: " + model + "), attempt " + attempt
                                                + "/"
                                                + maxRetries + "...");

                                String response = callGeminiApi(prompt);
                                if (response != null) {
                                        return response;
                                }

                                System.out.println("❌ No response from Gemini");
                                return "Không nhận được phản hồi từ AI. Vui lòng thử lại.";

                        } catch (WebClientResponseException e) {
                                String responseBody = e.getResponseBodyAsString();
                                System.err.println("❌ Gemini API Error (attempt " + attempt + "): "
                                                + e.getStatusCode() + " - " + responseBody);

                                // Check if rate limited (429 or RESOURCE_EXHAUSTED)
                                if (e.getStatusCode().value() == 429
                                                || responseBody.contains("RESOURCE_EXHAUSTED")) {
                                        if (attempt < maxRetries) {
                                                int waitSeconds = attempt * 20; // 20s, 40s, 60s
                                                System.out.println("⏳ Rate limited. Waiting " + waitSeconds
                                                                + "s before retry...");
                                                try {
                                                        Thread.sleep(waitSeconds * 1000L);
                                                } catch (InterruptedException ie) {
                                                        Thread.currentThread().interrupt();
                                                        break;
                                                }
                                                continue;
                                        }
                                        // All retries exhausted
                                        return "⏳ AI đang bận do giới hạn số lượng yêu cầu. Vui lòng đợi 1 phút rồi thử lại!\n\n"
                                                        + "Dưới đây là gợi ý phòng tự động:\n\n"
                                                        + getFallbackRecommendation(availableRooms, userRequest);
                                }

                                // Other API errors - don't retry
                                return "⚠️ Không thể kết nối đến AI. Vui lòng thử lại sau hoặc liên hệ lễ tân để được hỗ trợ.";

                        } catch (Exception e) {
                                System.err.println("❌ Error (attempt " + attempt + "): "
                                                + e.getClass().getSimpleName() + " - " + e.getMessage());

                                if (attempt >= maxRetries) {
                                        return "⚠️ Trợ lý AI hiện không khả dụng. Dưới đây là gợi ý phòng:\n\n"
                                                        + getFallbackRecommendation(availableRooms, userRequest);
                                }

                                // Wait before retry
                                try {
                                        Thread.sleep(5000L);
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        break;
                                }
                        }
                }

                return "⚠️ Trợ lý AI hiện không khả dụng. Dưới đây là gợi ý phòng:\n\n"
                                + getFallbackRecommendation(availableRooms, userRequest);
        }

        /**
         * Call Gemini API and parse the response
         */
        private String callGeminiApi(String prompt) {
                Map<String, Object> requestBody = Map.of(
                                "contents", List.of(
                                                Map.of("parts", List.of(
                                                                Map.of("text", prompt)))),
                                "generationConfig", Map.of(
                                                "temperature", 0.7,
                                                "maxOutputTokens", 1024));

                String uri = String.format("/v1beta/models/%s:generateContent?key=%s", model, apiKey);

                @SuppressWarnings("unchecked")
                Map<String, Object> result = webClient.post()
                                .uri(uri)
                                .header("Content-Type", "application/json")
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .timeout(Duration.ofSeconds(60))
                                .block();

                if (result != null && result.containsKey("candidates")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> candidates = (List<Map<String, Object>>) result
                                        .get("candidates");
                        if (!candidates.isEmpty()) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> content = (Map<String, Object>) candidates.get(0)
                                                .get("content");
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> parts = (List<Map<String, Object>>) content
                                                .get("parts");
                                if (!parts.isEmpty()) {
                                        String response = (String) parts.get(0).get("text");
                                        System.out.println("✅ Gemini Response received: "
                                                        + response.substring(0, Math.min(100, response.length()))
                                                        + "...");
                                        return response;
                                }
                        }
                }
                return null;
        }

        /**
         * Fallback recommendation when Gemini is not available
         * Uses simple keyword matching to suggest rooms
         */
        private String getFallbackRecommendation(List<Room> rooms, String userRequest) {
                String request = userRequest.toLowerCase();
                StringBuilder sb = new StringBuilder();

                List<Room> suggestions;
                if (request.contains("vip") || request.contains("sang trọng") || request.contains("cao cấp")) {
                        suggestions = rooms.stream()
                                        .filter(r -> r.getRoomType().getName().contains("VIP")
                                                        || r.getRoomType().getName().contains("SUITE"))
                                        .limit(2)
                                        .toList();
                } else if (request.contains("gia đình") || request.contains("4 người") || request.contains("nhóm")) {
                        suggestions = rooms.stream()
                                        .filter(r -> r.getRoomType().getName().contains("FAMILY")
                                                        || r.getRoomType().getName().contains("DELUXE"))
                                        .limit(2)
                                        .toList();
                } else if (request.contains("cặp đôi") || request.contains("lãng mạn")
                                || request.contains("2 người")) {
                        suggestions = rooms.stream()
                                        .filter(r -> r.getRoomType().getName().contains("DOUBLE")
                                                        || r.getRoomType().getName().contains("DELUXE"))
                                        .limit(2)
                                        .toList();
                } else if (request.contains("rẻ") || request.contains("tiết kiệm") || request.contains("hợp lý")) {
                        suggestions = rooms.stream()
                                        .sorted((a, b) -> a.getPricePerNight().compareTo(b.getPricePerNight()))
                                        .limit(2)
                                        .toList();
                } else {
                        suggestions = rooms.stream().limit(2).toList();
                }

                if (suggestions.isEmpty()) {
                        suggestions = rooms.stream().limit(2).toList();
                }

                for (Room room : suggestions) {
                        sb.append(String.format("🏨 Phòng %s (%s)\n", room.getRoomNumber(),
                                        room.getRoomType().getDisplayName()));
                        sb.append(String.format("   Giá: %s VNĐ/đêm\n", room.getPricePerNight().toString()));
                        if (room.getDescription() != null && !room.getDescription().isEmpty()) {
                                sb.append(String.format("   %s\n", room.getDescription()));
                        }
                        sb.append("\n");
                }

                sb.append("💡 Để được tư vấn chi tiết hơn, vui lòng liên hệ lễ tân hoặc thử lại sau!");
                return sb.toString();
        }
}
