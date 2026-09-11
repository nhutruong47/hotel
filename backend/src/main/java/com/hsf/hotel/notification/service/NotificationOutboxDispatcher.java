package com.hsf.hotel.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.config.RabbitMQConfig;
import com.hsf.hotel.notification.dto.NotificationEvent;
import com.hsf.hotel.notification.model.NotificationOutbox;
import com.hsf.hotel.notification.repository.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.notification.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxDispatcher.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_BACKOFF_SECONDS = 300;
    private static final long PUBLISH_CONFIRM_TIMEOUT_MS = 5_000;

    private final NotificationOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final int retentionDays;

    public NotificationOutboxDispatcher(NotificationOutboxRepository outboxRepository,
                                        RabbitTemplate rabbitTemplate,
                                        ObjectMapper objectMapper,
                                        @Value("${app.notification.outbox.retention-days:7}") int retentionDays) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(
            fixedDelayString = "${app.notification.outbox.poll-ms:5000}",
            initialDelayString = "${app.notification.outbox.initial-delay-ms:5000}")
    @Transactional
    public void publishReadyEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationOutbox> rows = outboxRepository.findReadyForUpdate(
                List.of(NotificationOutbox.OutboxStatus.PENDING,
                        NotificationOutbox.OutboxStatus.FAILED),
                now,
                PageRequest.of(0, BATCH_SIZE));

        for (NotificationOutbox row : rows) {
            publish(row, now);
        }
    }

    @Scheduled(cron = "${app.notification.outbox.cleanup-cron:0 30 2 * * *}")
    @Transactional
    public void cleanupPublishedEvents() {
        int deleted = outboxRepository.deletePublishedBefore(
                NotificationOutbox.OutboxStatus.PUBLISHED,
                LocalDateTime.now().minusDays(retentionDays));
        if (deleted > 0) {
            log.info("Deleted {} published notification outbox rows", deleted);
        }
    }

    private void publish(NotificationOutbox row, LocalDateTime now) {
        try {
            if (row.getPayload() == null || row.getPayload().isBlank()) {
                throw new IllegalStateException("Outbox payload is missing");
            }
            NotificationEvent event = objectMapper.readValue(row.getPayload(), NotificationEvent.class);
            rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        row.getRoutingKey(),
                        event,
                        message -> {
                            message.getMessageProperties().setContentType("application/json");
                            message.getMessageProperties().setHeader("X-Outbox-Id", row.getId());
                            return message;
                        });
                operations.waitForConfirmsOrDie(PUBLISH_CONFIRM_TIMEOUT_MS);
                return null;
            });
            row.setStatus(NotificationOutbox.OutboxStatus.PUBLISHED);
            row.setPublishedAt(now);
            row.setLastError(null);
            // Raw verification/reset tokens must not remain at rest after the
            // broker accepted the message.
            row.setPayload(null);
        } catch (Exception ex) {
            int attempts = row.getAttempts() + 1;
            row.setAttempts(attempts);
            row.setStatus(NotificationOutbox.OutboxStatus.FAILED);
            row.setLastError(truncate(ex.getMessage()));
            long delay = Math.min(MAX_BACKOFF_SECONDS, 1L << Math.min(attempts, 8));
            row.setAvailableAt(now.plusSeconds(delay));
            log.warn("Notification outbox event {} publish failed (attempt {}): {}",
                    row.getId(), attempts, ex.getMessage());
        }
    }

    private static String truncate(String value) {
        if (value == null) return "Unknown broker error";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
