package com.hsf.hotel.service;

import com.hsf.hotel.model.Room;
import com.hsf.hotel.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RATE_LIMIT_BASE_WAIT_MS = 20_000L;
    private static final long GENERIC_RETRY_WAIT_MS = 5_000L;

    private final String apiKey;
    private final String model;
    private final WebClient webClient;
    private final RoomRepository roomRepository;

    public GeminiService(@Value("${gemini.api.key:}") String apiKey,
                         @Value("${gemini.api.model:gemini-2.0-flash}") String model,
                         RoomRepository roomRepository) {
        this.apiKey = apiKey;
        this.model = model;
        this.roomRepository = roomRepository;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Transactional(readOnly = true)
    public String getAiRecommendation(String userRequest) {
        log.info("AI request received (length={})", userRequest == null ? 0 : userRequest.length());

        List<Room> availableRooms = roomRepository.findByIsAvailableTrue();
        log.info("Found {} available rooms", availableRooms.size());
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

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String response = callGeminiApi(prompt);
                if (response != null) {
                    return response;
                }
                log.warn("No response from Gemini on attempt {}", attempt);
                return "Không nhận được phản hồi từ AI. Vui lòng thử lại.";
            } catch (WebClientResponseException e) {
                String body = e.getResponseBodyAsString();
                log.warn("Gemini API error attempt {}: {} - {}", attempt, e.getStatusCode(), body);
                if (e.getStatusCode().value() == 429 || body.contains("RESOURCE_EXHAUSTED")) {
                    if (attempt < MAX_RETRIES) {
                        sleep(RATE_LIMIT_BASE_WAIT_MS * attempt);
                        continue;
                    }
                    return rateLimitFallback(availableRooms, userRequest);
                }
                return "Khong thể kết nối đến AI. Vui lòng thử lại sau hoặc liên hệ lễ tân để được hỗ trợ."
                        .replace("Khong", "Không");
            } catch (Exception e) {
                log.warn("Error on attempt {}: {} - {}", attempt, e.getClass().getSimpleName(), e.getMessage());
                if (attempt >= MAX_RETRIES) {
                    return genericFallback(availableRooms, userRequest);
                }
                sleep(GENERIC_RETRY_WAIT_MS);
            }
        }
        return genericFallback(availableRooms, userRequest);
    }

    private String callGeminiApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 1024)
        );
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
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
            if (!candidates.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (!parts.isEmpty()) {
                    String response = (String) parts.get(0).get("text");
                    log.info("Gemini response received (length={})",
                            response == null ? 0 : response.length());
                    return response;
                }
            }
        }
        return null;
    }

    private String rateLimitFallback(List<Room> rooms, String request) {
        return "AI đang bận do giới hạn số lượng yêu cầu. Vui lòng đợi 1 phút rồi thử lại!\n\n"
                + "Dưới đây là gợi ý phòng tự động:\n\n"
                + renderFallback(rooms, request);
    }

    private String genericFallback(List<Room> rooms, String request) {
        return "Trợ lý AI hiện không khả dụng. Dưới đây là gợi ý phòng:\n\n"
                + renderFallback(rooms, request);
    }

    private String renderFallback(List<Room> rooms, String userRequest) {
        String r = userRequest == null ? "" : userRequest.toLowerCase();
        List<Room> suggestions;
        if (r.contains("vip") || r.contains("sang trọng") || r.contains("cao cấp")) {
            suggestions = rooms.stream()
                    .filter(x -> x.getRoomType().getName().contains("VIP")
                            || x.getRoomType().getName().contains("SUITE"))
                    .limit(2).toList();
        } else if (r.contains("gia đình") || r.contains("4 người") || r.contains("nhóm")) {
            suggestions = rooms.stream()
                    .filter(x -> x.getRoomType().getName().contains("FAMILY")
                            || x.getRoomType().getName().contains("DELUXE"))
                    .limit(2).toList();
        } else if (r.contains("cặp đôi") || r.contains("lãng mạn") || r.contains("2 người")) {
            suggestions = rooms.stream()
                    .filter(x -> x.getRoomType().getName().contains("DOUBLE")
                            || x.getRoomType().getName().contains("DELUXE"))
                    .limit(2).toList();
        } else if (r.contains("rẻ") || r.contains("tiết kiệm") || r.contains("hợp lý")) {
            suggestions = rooms.stream()
                    .sorted((a, b) -> a.getPricePerNight().compareTo(b.getPricePerNight()))
                    .limit(2).toList();
        } else {
            suggestions = rooms.stream().limit(2).toList();
        }
        if (suggestions.isEmpty()) {
            suggestions = rooms.stream().limit(2).toList();
        }

        StringBuilder sb = new StringBuilder();
        for (Room room : suggestions) {
            sb.append(String.format("Phòng %s (%s)%n",
                    room.getRoomNumber(), room.getRoomType().getDisplayName()));
            sb.append(String.format("   Giá: %s VNĐ/đêm%n", room.getPricePerNight()));
            if (room.getDescription() != null && !room.getDescription().isEmpty()) {
                sb.append(String.format("   %s%n", room.getDescription()));
            }
            sb.append("\n");
        }
        sb.append("Để được tư vấn chi tiết hơn, vui lòng liên hệ lễ tân hoặc thử lại sau!");
        return sb.toString();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
