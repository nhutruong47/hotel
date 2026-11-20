package com.hsf.hotel.admin.repository;

import com.hsf.hotel.admin.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    Page<AuditLog> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Integer entityId);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start,
                                                              LocalDateTime end,
                                                              Pageable pageable);

    /** Used for time-bound retention sweeps. */
    long deleteByCreatedAtBefore(LocalDateTime cutoff);

    /** Retention sweep for operational rows: everything except the given action prefix (financial). */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "DELETE FROM AuditLog a WHERE a.createdAt < :cutoff AND (a.action IS NULL OR a.action NOT LIKE :prefix)")
    int deleteOperationalBefore(@org.springframework.data.repository.query.Param("cutoff") LocalDateTime cutoff,
                                @org.springframework.data.repository.query.Param("prefix") String excludedPrefix);

    /** Retention sweep for financial rows (action LIKE prefix), e.g. after 10 years. */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "DELETE FROM AuditLog a WHERE a.createdAt < :cutoff AND a.action LIKE :prefix")
    int deleteFinancialBefore(@org.springframework.data.repository.query.Param("cutoff") LocalDateTime cutoff,
                              @org.springframework.data.repository.query.Param("prefix") String financialPrefix);
}
