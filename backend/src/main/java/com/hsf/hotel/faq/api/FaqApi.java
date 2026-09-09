package com.hsf.hotel.faq.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ApiResponses;
import com.hsf.hotel.faq.dto.FaqResponse;
import com.hsf.hotel.faq.model.FaqItem.FaqCategory;
import com.hsf.hotel.faq.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/faqs")
@RequiredArgsConstructor
public class FaqApi {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getAllFaqs() {
        return ApiResponses.ok(map(faqService.getAllPublishedFaqs()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getFaqsByCategory(@PathVariable FaqCategory category) {
        return ApiResponses.ok(map(faqService.getFaqsByCategory(category)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<FaqCategory>>> getCategories() {
        return ApiResponses.ok(faqService.getActiveCategories());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FaqResponse>>> searchFaqs(@RequestParam String query) {
        return ApiResponses.ok(map(faqService.searchFaqs(query)));
    }

    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<Map<FaqCategory, List<FaqResponse>>>> getGroupedFaqs() {
        Map<FaqCategory, List<FaqResponse>> grouped = faqService.getFaqsGroupedByCategory().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> map(entry.getValue())));
        return ApiResponses.ok(grouped);
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<ApiResponse<FaqResponse>> markHelpful(
            @PathVariable Integer id, @RequestParam boolean helpful) {
        return ApiResponses.ok(FaqResponse.from(faqService.markHelpful(id, helpful)));
    }

    private static List<FaqResponse> map(List<com.hsf.hotel.faq.model.FaqItem> items) {
        return items.stream().map(FaqResponse::from).toList();
    }
}
