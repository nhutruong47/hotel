package com.hsf.hotel.common.audit;

import java.time.LocalDateTime;

/**
 * Entity that records who created / last changed it (V19, database-design.md §5).
 * Implementations decide which columns they own — some tables already had a
 * {@code created_at} or a {@code created_by} association before V19.
 */
public interface AuditStamped {

    /** Called once before insert. {@code actorUserId} is null for system/scheduler writes. */
    void onCreateStamp(LocalDateTime now, Integer actorUserId);

    /** Called before every update. {@code actorUserId} is null for system/scheduler writes. */
    void onUpdateStamp(LocalDateTime now, Integer actorUserId);
}
