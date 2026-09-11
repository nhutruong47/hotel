package com.hsf.hotel.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.config.RabbitMQConfig;
import com.hsf.hotel.notification.dto.NotificationEvent;
import com.hsf.hotel.notification.model.NotificationOutbox;
import com.hsf.hotel.notification.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxTest {

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void producerPersistsEventInsteadOfPublishingInsideBusinessTransaction() {
        NotificationProducer producer = new NotificationProducer(outboxRepository, objectMapper);
        NotificationEvent event = new NotificationEvent("PAYMENT_CUSTOMER", 42);

        producer.sendEmailNotification(event);

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(outboxRepository).save(captor.capture());
        verifyNoInteractions(rabbitTemplate);
        NotificationOutbox row = captor.getValue();
        assertEquals("PAYMENT_CUSTOMER", row.getEventType());
        assertEquals("hotel.email.PAYMENT_CUSTOMER", row.getRoutingKey());
        assertTrue(row.getPayload().contains("\"targetId\":42"));
        assertEquals(NotificationOutbox.OutboxStatus.PENDING, row.getStatus());
    }

    @Test
    void dispatcherPublishesAndClearsSensitivePayload() throws Exception {
        NotificationEvent event = new NotificationEvent("VERIFICATION", 7);
        event.setUserId(7);
        event.setRawToken("raw-secret-token");
        NotificationOutbox row = rowFor(event);
        when(outboxRepository.findReadyForUpdate(anyCollection(), any(), any())).thenReturn(List.of(row));
        invokeRabbitCallback();
        NotificationOutboxDispatcher dispatcher =
                new NotificationOutboxDispatcher(outboxRepository, rabbitTemplate, objectMapper, 7);

        dispatcher.publishReadyEvents();

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("hotel.email.VERIFICATION"),
                argThat(message -> message instanceof NotificationEvent notification
                        && "raw-secret-token".equals(notification.getRawToken())),
                any(MessagePostProcessor.class));
        assertEquals(NotificationOutbox.OutboxStatus.PUBLISHED, row.getStatus());
        assertNotNull(row.getPublishedAt());
        assertNull(row.getPayload());
    }

    @Test
    void dispatcherRetainsPayloadAndSchedulesRetryWhenBrokerFails() throws Exception {
        NotificationEvent event = new NotificationEvent("PAYMENT_ADMIN", 9);
        NotificationOutbox row = rowFor(event);
        row.setAvailableAt(LocalDateTime.now().minusSeconds(1));
        when(outboxRepository.findReadyForUpdate(anyCollection(), any(), any())).thenReturn(List.of(row));
        invokeRabbitCallback();
        doThrow(new AmqpException("broker unavailable")).when(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(), any(MessagePostProcessor.class));
        NotificationOutboxDispatcher dispatcher =
                new NotificationOutboxDispatcher(outboxRepository, rabbitTemplate, objectMapper, 7);

        dispatcher.publishReadyEvents();

        assertEquals(NotificationOutbox.OutboxStatus.FAILED, row.getStatus());
        assertEquals(1, row.getAttempts());
        assertNotNull(row.getPayload());
        assertTrue(row.getLastError().contains("broker unavailable"));
        assertTrue(row.getAvailableAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void cleanupOnlyDeletesPublishedRowsOlderThanRetention() {
        NotificationOutboxDispatcher dispatcher =
                new NotificationOutboxDispatcher(outboxRepository, rabbitTemplate, objectMapper, 7);

        dispatcher.cleanupPublishedEvents();

        verify(outboxRepository).deletePublishedBefore(
                eq(NotificationOutbox.OutboxStatus.PUBLISHED),
                argThat(cutoff -> cutoff.isBefore(LocalDateTime.now().minusDays(6))));
    }

    private NotificationOutbox rowFor(NotificationEvent event) throws Exception {
        NotificationOutbox row = new NotificationOutbox();
        row.setEventType(event.getType());
        row.setRoutingKey("hotel.email." + event.getType());
        row.setPayload(objectMapper.writeValueAsString(event));
        return row;
    }

    @SuppressWarnings("unchecked")
    private void invokeRabbitCallback() {
        when(rabbitTemplate.invoke(any(RabbitOperations.OperationsCallback.class)))
                .thenAnswer(invocation -> {
                    RabbitOperations.OperationsCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInRabbit(rabbitTemplate);
                });
    }
}
