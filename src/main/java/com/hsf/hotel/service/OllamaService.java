package com.hsf.hotel.service;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OllamaService {

        private final WebClient webClient;

        @Autowired
        private RoomRepository roomRepository;

        public OllamaService() {
                this.webClient = WebClient.builder()
                                .baseUrl("http://localhost:11434")
                                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                                .build();
        }

        public String getAiRecommendation(String userRequest) {
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

                try {
                        System.out.println("🚀 Calling Ollama API...");

                        Map<String, Object> requestBody = Map.of(
                                        "model", "qwen2.5:7b",
                                        "prompt", prompt,
                                        "stream", false);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = webClient.post()
                                        .uri("/api/generate")
                                        .bodyValue(requestBody)
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .timeout(Duration.ofSeconds(120))
                                        .block();

                        if (result != null && result.containsKey("response")) {
                                String response = (String) result.get("response");
                                System.out.println("✅ AI Response received: "
                                                + response.substring(0, Math.min(100, response.length())) + "...");
                                return response;
                        } else {
                                System.out.println("❌ No response from Ollama");
                                return "Không nhận được phản hồi từ AI. Vui lòng thử lại.";
                        }
                } catch (WebClientResponseException e) {
                        System.err.println("❌ Ollama API Error: " + e.getStatusCode() + " - " + e.getMessage());
                        return "⚠️ Không thể kết nối đến AI. Vui lòng thử lại sau hoặc liên hệ lễ tân để được hỗ trợ.";
                } catch (Exception e) {
                        System.err.println("❌ Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        // Return friendly fallback message instead of technical error
                        return "⚠️ Trợ lý AI hiện không khả dụng. Dưới đây là gợi ý phòng:\n\n"
                                        + getFallbackRecommendation(availableRooms, userRequest);
                }
        }

        /**
         * Fallback recommendation when Ollama is not available
         * Uses simple keyword matching to suggest rooms
         */
        private String getFallbackRecommendation(List<Room> rooms, String userRequest) {
                String request = userRequest.toLowerCase();
                StringBuilder sb = new StringBuilder();

                // Simple keyword matching
                List<Room> suggestions;
                if (request.contains("vip") || request.contains("sang trọng") || request.contains("cao cấp")) {
                        suggestions = rooms.stream()
                                        .filter(r -> r.getRoomType().name().contains("VIP")
                                                        || r.getRoomType().name().contains("SUITE"))
                                        .limit(2)
                                        .toList();
                } else if (request.contains("gia đình") || request.contains("4 người") || request.contains("nhóm")) {
                        suggestions = rooms.stream()
                                        .filter(r -> r.getRoomType().name().contains("FAMILY")
                                                        || r.getRoomType().name().contains("DELUXE"))
                                        .limit(2)
                                        .toList();
                } else if (request.contains("cặp đôi") || request.contains("lãng mạn") || request.contains("2 người")) {
                        suggestions = rooms.stream()
                                        .filter(r -> r.getRoomType().name().contains("DOUBLE")
                                                        || r.getRoomType().name().contains("DELUXE"))
                                        .limit(2)
                                        .toList();
                } else if (request.contains("rẻ") || request.contains("tiết kiệm") || request.contains("hợp lý")) {
                        suggestions = rooms.stream()
                                        .sorted((a, b) -> a.getPricePerNight().compareTo(b.getPricePerNight()))
                                        .limit(2)
                                        .toList();
                } else {
                        // Default: return first 2 available rooms
                        suggestions = rooms.stream().limit(2).toList();
                }

                if (suggestions.isEmpty()) {
                        suggestions = rooms.stream().limit(2).toList();
                }

                for (Room room : suggestions) {
                        sb.append(String.format("🏨 **Phòng %s** (%s)\n", room.getRoomNumber(),
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
