package com.hsf.hotel.faq.repository;

import com.hsf.hotel.faq.model.FaqItem;
import com.hsf.hotel.faq.model.FaqItem.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqItemRepository extends JpaRepository<FaqItem, Integer> {

    List<FaqItem> findByIsPublishedTrueOrderByDisplayOrderAsc();

    List<FaqItem> findByCategoryAndIsPublishedTrueOrderByDisplayOrderAsc(FaqCategory category);

    @Query("SELECT f FROM FaqItem f WHERE f.isPublished = true AND " +
           "(LOWER(f.question) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(f.answer) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(f.metaKeywords) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\')")
    List<FaqItem> searchFaqItems(@Param("query") String query);

    List<FaqItem> findByCategoryOrderByDisplayOrderAsc(FaqCategory category);

    @Query("SELECT DISTINCT f.category FROM FaqItem f WHERE f.isPublished = true")
    List<FaqCategory> findActiveCategories();

    long countByIsPublishedTrue();
}
