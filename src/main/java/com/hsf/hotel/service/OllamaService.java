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
                        return "Lỗi API Ollama: " + e.getStatusCode();
                } catch (Exception e) {
                        System.err.println("❌ Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        e.printStackTrace();
                        return "Lỗi kết nối AI: " + e.getMessage();
                }
        }
}
