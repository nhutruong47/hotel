package com.hsf.hotel.notification.repository;

import com.hsf.hotel.notification.model.NotificationOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM NotificationOutbox o "
            + "WHERE o.status IN :statuses AND o.availableAt <= :now "
            + "ORDER BY o.createdAt ASC")
    List<NotificationOutbox> findReadyForUpdate(
            @Param("statuses") Collection<NotificationOutbox.OutboxStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Modifying
    @Query("DELETE FROM NotificationOutbox o WHERE o.status = :status AND o.publishedAt < :cutoff")
    int deletePublishedBefore(
            @Param("status") NotificationOutbox.OutboxStatus status,
            @Param("cutoff") LocalDateTime cutoff);
}
