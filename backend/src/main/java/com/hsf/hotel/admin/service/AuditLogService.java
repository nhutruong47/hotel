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

    /**
     * Removes audit rows older than {@link #RETENTION_DAYS}. Returns the
     * number of rows deleted so the scheduler can log the result.
     */
    @Transactional
    public long purgeExpiredLogs() {
        long deleted = auditLogRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minusDays(RETENTION_DAYS));
        if (deleted > 0) {
            log.info("Purged {} audit-log rows older than {} days", deleted, RETENTION_DAYS);
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
