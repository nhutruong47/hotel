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

        @Value("${gemini.api.key}")
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
                System.out.println("🤖 AI Request: " + userRequest);

                // Get all available rooms to provide context to AI
                List<Room> availableRooms = roomRepository.findByIsAvailableTrue();
                System.out.println("📋 Found " + availableRooms.size() + " available rooms");

                if (availableRooms.isEmpty()) {
                        return "No rooms are currently available. Please come back later!";
                }

                String roomsContext = availableRooms.stream()
                                .map(room -> String.format(
                                                "- Room %s (%s): %s - Price: %s VND/night",
                                                room.getRoomNumber(),
                                                room.getRoomType().getDisplayName(),
                                                room.getDescription() != null ? room.getDescription() : "",
                                                room.getPricePerNight().toString()))
                                .collect(Collectors.joining("\n"));

                String prompt = String.format("""
                                You are an AI assistant of Nhu Hotel. Help guests choose a suitable room.

                                AVAILABLE ROOMS:
                                %s

                                GUEST NEEDS: %s

                                Suggest 1-2 most suitable rooms, explain briefly. Answer in English.
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
                                return "No response received from AI. Please try again.";

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
                                        return "⏳ AI is busy due to request limits. Please wait 1 minute and try again!\n\n"
                                                        + "Here are automatic room suggestions:\n\n"
                                                        + getFallbackRecommendation(availableRooms, userRequest);
                                }

                                // Other API errors - don't retry
                                return "⚠️ Cannot connect to AI. Please try again later or contact the receptionist for support.";

                        } catch (Exception e) {
                                System.err.println("❌ Error (attempt " + attempt + "): "
                                                + e.getClass().getSimpleName() + " - " + e.getMessage());

                                if (attempt >= maxRetries) {
                                        return "⚠️ AI assistant is currently unavailable. Here are some room suggestions:\n\n"
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

                return "⚠️ AI assistant is currently unavailable. Here are some room suggestions:\n\n"
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
                        sb.append(String.format("🏨 Room %s (%s)\n", room.getRoomNumber(),
                                        room.getRoomType().getDisplayName()));
                        sb.append(String.format("   Price: %s VND/night\n", room.getPricePerNight().toString()));
                        if (room.getDescription() != null && !room.getDescription().isEmpty()) {
                                sb.append(String.format("   %s\n", room.getDescription()));
                        }
                        sb.append("\n");
                }

                sb.append("💡 For more detailed advice, please contact the receptionist or try again later!");
                return sb.toString();
        }
}
