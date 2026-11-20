package com.hsf.hotel.admin.service;

import com.hsf.hotel.admin.model.AuditLog;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.admin.repository.AuditLogRepository;
import com.hsf.hotel.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Records security-relevant operations for compliance and debugging.
 * Uses {@link Propagation#REQUIRES_NEW} so that audit records are persisted
 * even if the surrounding business transaction rolls back.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    /** Retention window for log rows. Older rows are pruned by a scheduled job. */
    public static final int RETENTION_DAYS = 180;
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(password|passwd|pwd|token|secret|api[-_]?key|stripe[-_]?signature|authorization)\\s*[:=]\\s*[^\\s,;]+");

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, String action, String entityType, Integer entityId, String details, HttpServletRequest request) {
        String safeDetails = sanitize(details);
        AuditLog entry = new AuditLog(
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : "SYSTEM",
                action,
                entityType,
                entityId,
                safeDetails
        );
        if (request != null) {
            entry.setIpAddress(ClientIpResolver.resolve(request));
            entry.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        }
        auditLogRepository.save(entry);
        log.info("AUDIT: {} by {} on {}#{} — {}", action,
                entry.getUsername(), entityType, entityId, safeDetails);
    }

    /** Convenience overload without HTTP request context. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, String action, String entityType, Integer entityId, String details) {
        log(user, action, entityType, entityId, details, null);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getRecentLogs(Pageable pageable) {
        return auditLogRepository.findByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByUser(Integer userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<AuditLog> getLogsByEntity(String entityType, Integer entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    /** Financial audit rows (payments/refunds) are accounting records: kept 10 years (ADR-0009 Q9). */
    public static final String FINANCIAL_ACTION_PREFIX = "PAYMENT_";
    public static final int FINANCIAL_RETENTION_YEARS = 10;

    /**
     * Retention sweep (ADR-0009 Q9): operational rows older than {@link #RETENTION_DAYS}
     * days; financial rows ({@code PAYMENT_*}) only after {@link #FINANCIAL_RETENTION_YEARS}
     * years (Accounting Law 2015 art. 41). Returns the number of rows deleted.
     */
    @Transactional
    public long purgeExpiredLogs() {
        LocalDateTime now = LocalDateTime.now();
        long operational = auditLogRepository.deleteOperationalBefore(
                now.minusDays(RETENTION_DAYS), FINANCIAL_ACTION_PREFIX + "%");
        long financial = auditLogRepository.deleteFinancialBefore(
                now.minusYears(FINANCIAL_RETENTION_YEARS), FINANCIAL_ACTION_PREFIX + "%");
        long deleted = operational + financial;
        if (deleted > 0) {
            log.info("Purged {} operational audit rows (> {} days) and {} financial rows (> {} years)",
                    operational, RETENTION_DAYS, financial, FINANCIAL_RETENTION_YEARS);
        }
        return deleted;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace('\r', ' ').replace('\n', ' ');
        cleaned = EMAIL.matcher(cleaned).replaceAll("[email-redacted]");
        cleaned = SECRET_ASSIGNMENT.matcher(cleaned).replaceAll("$1=[redacted]");
        return truncate(cleaned, 1000);
    }
}
