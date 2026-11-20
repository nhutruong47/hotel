package com.hsf.hotel.common.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/**
 * JPA entity listener filling {@code created_*}/{@code updated_*} audit columns
 * (V19). The acting user id is published by {@code SessionAuthBridgeFilter} as the
 * authentication details; outside a request (scheduler, seeding) it is null.
 *
 * <p>Deliberately not Spring Data auditing: several entities pre-date V19 with
 * their own {@code createdAt} handling or a {@code created_by} association, and a
 * per-entity hook lets each keep its existing semantics.
 */
public class AuditStampListener {

    @PrePersist
    public void beforeInsert(Object entity) {
        if (entity instanceof AuditStamped stamped) {
            stamped.onCreateStamp(LocalDateTime.now(), currentUserId());
        }
    }

    @PreUpdate
    public void beforeUpdate(Object entity) {
        if (entity instanceof AuditStamped stamped) {
            stamped.onUpdateStamp(LocalDateTime.now(), currentUserId());
        }
    }

    /** Id of the authenticated user of the current request, or null. */
    public static Integer currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getDetails() instanceof Integer id) {
            return id;
        }
        return null;
    }
}
