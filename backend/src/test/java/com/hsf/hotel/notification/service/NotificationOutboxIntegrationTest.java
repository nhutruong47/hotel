package com.hsf.hotel.notification.service;

import com.hsf.hotel.notification.dto.NotificationEvent;
import com.hsf.hotel.notification.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationOutboxIntegrationTest {

    @Autowired
    private NotificationProducer producer;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean() {
        outboxRepository.deleteAll();
    }

    @Test
    void eventCommitsWithBusinessTransaction() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                producer.sendEmailNotification(new NotificationEvent("PAYMENT_CUSTOMER", 1)));

        assertEquals(1, outboxRepository.count());
    }

    @Test
    void rolledBackBusinessTransactionDoesNotLeavePhantomEvent() {
        assertThrows(IllegalStateException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    producer.sendEmailNotification(new NotificationEvent("PAYMENT_CUSTOMER", 1));
                    throw new IllegalStateException("force rollback");
                }));

        assertEquals(0, outboxRepository.count());
    }
}
