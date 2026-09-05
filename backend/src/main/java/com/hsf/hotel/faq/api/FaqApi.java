package com.hsf.hotel.faq.api;

import com.hsf.hotel.faq.model.FaqItem;
import com.hsf.hotel.faq.model.FaqItem.FaqCategory;
import com.hsf.hotel.faq.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqApi {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<List<FaqItem>> getAllFaqs() {
        return ResponseEntity.ok(faqService.getAllPublishedFaqs());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FaqItem>> getFaqsByCategory(@PathVariable FaqCategory category) {
        return ResponseEntity.ok(faqService.getFaqsByCategory(category));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<FaqCategory>> getCategories() {
        return ResponseEntity.ok(faqService.getActiveCategories());
    }

    @GetMapping("/search")
    public ResponseEntity<List<FaqItem>> searchFaqs(@RequestParam String query) {
        return ResponseEntity.ok(faqService.searchFaqs(query));
    }

    @GetMapping("/grouped")
    public ResponseEntity<Map<FaqCategory, List<FaqItem>>> getGroupedFaqs() {
        return ResponseEntity.ok(faqService.getFaqsGroupedByCategory());
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<FaqItem> markHelpful(@PathVariable Integer id, @RequestParam boolean helpful) {
        return ResponseEntity.ok(faqService.markHelpful(id, helpful));
    }
}
