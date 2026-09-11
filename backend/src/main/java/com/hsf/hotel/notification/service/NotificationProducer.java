package com.hsf.hotel.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.notification.dto.NotificationEvent;
import com.hsf.hotel.notification.model.NotificationOutbox;
import com.hsf.hotel.notification.repository.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final NotificationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public NotificationProducer(NotificationOutboxRepository outboxRepository,
                                ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void sendEmailNotification(NotificationEvent event) {
        if (event == null || event.getType() == null || event.getType().isBlank()) {
            throw new IllegalArgumentException("Notification event type is required");
        }
        try {
            NotificationOutbox row = new NotificationOutbox();
            row.setEventType(event.getType());
            row.setRoutingKey("hotel.email." + event.getType());
            row.setPayload(objectMapper.writeValueAsString(event));
            outboxRepository.save(row);
            log.info("Queued durable email notification: type={}, targetId={}",
                    event.getType(), event.getTargetId());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize notification event", ex);
        }
    }
}
