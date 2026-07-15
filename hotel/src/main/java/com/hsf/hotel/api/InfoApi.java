package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/info")
public class InfoApi {

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse> getInfoPage(@PathVariable String slug) {
        Map<String, Object> content = new HashMap<>();
        content.put("slug", slug);
        content.put("title", formatTitle(slug));
        content.put("content",
                "This is placeholder content for the " + slug + " page. In a real system, this would be fetched from a CMS or Database.");
        return ResponseEntity.ok(ApiResponse.ok(content));
    }

    private static String formatTitle(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "";
        }
        return slug.substring(0, 1).toUpperCase() + slug.substring(1).replace("-", " ");
    }
}
