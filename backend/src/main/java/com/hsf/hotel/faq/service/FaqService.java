package com.hsf.hotel.faq.service;

import com.hsf.hotel.faq.model.FaqItem;
import com.hsf.hotel.faq.model.FaqItem.FaqCategory;
import com.hsf.hotel.faq.repository.FaqItemRepository;
import com.hsf.hotel.security.Sanitizers;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqItemRepository faqRepository;

    @Cacheable(cacheNames = "faqs", key = "'allPublished'")
    public List<FaqItem> getAllPublishedFaqs() {
        return faqRepository.findByIsPublishedTrueOrderByDisplayOrderAsc();
    }

    @Cacheable(cacheNames = "faqs", key = "'category_' + #category.name()")
    public List<FaqItem> getFaqsByCategory(FaqCategory category) {
        return faqRepository.findByCategoryAndIsPublishedTrueOrderByDisplayOrderAsc(category);
    }

    public List<FaqItem> searchFaqs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPublishedFaqs();
        }
        // Escape SQL LIKE meta-characters so a user searching for "100%"
        // does not match every row.
        String sanitised = Sanitizers.escapeLikePattern(query.trim());
        return faqRepository.searchFaqItems(sanitised);
    }

    @Cacheable(cacheNames = "faqs", key = "'grouped'")
    public Map<FaqCategory, List<FaqItem>> getFaqsGroupedByCategory() {
        List<FaqItem> faqs = getAllPublishedFaqs();
        return faqs.stream()
                .collect(Collectors.groupingBy(FaqItem::getCategory));
    }

    @Cacheable(cacheNames = "faqs", key = "'categories'")
    public List<FaqCategory> getActiveCategories() {
        return faqRepository.findActiveCategories();
    }

    @Transactional
    @CacheEvict(cacheNames = "faqs", allEntries = true)
    public FaqItem markHelpful(Integer id, boolean helpful) {
        FaqItem faq = faqRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FAQ not found: " + id));
        if (helpful) {
            faq.incrementHelpful();
        } else {
            faq.incrementNotHelpful();
        }
        return faqRepository.save(faq);
    }

    @Transactional
    @CacheEvict(cacheNames = "faqs", allEntries = true)
    public FaqItem createFaq(FaqItem faq) {
        faq.setCreatedAt(LocalDateTime.now());
        return faqRepository.save(faq);
    }

    @Transactional
    @CacheEvict(cacheNames = "faqs", allEntries = true)
    public FaqItem updateFaq(Integer id, FaqItem updated) {
        FaqItem existing = faqRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FAQ not found: " + id));
        existing.setQuestion(updated.getQuestion());
        existing.setAnswer(updated.getAnswer());
        existing.setCategory(updated.getCategory());
        existing.setDisplayOrder(updated.getDisplayOrder());
        existing.setIsPublished(updated.getIsPublished());
        existing.setMetaKeywords(updated.getMetaKeywords());
        existing.setUpdatedAt(LocalDateTime.now());
        return faqRepository.save(existing);
    }

    @CacheEvict(cacheNames = "faqs", allEntries = true)
    public void deleteFaq(Integer id) {
        faqRepository.deleteById(id);
    }
}
