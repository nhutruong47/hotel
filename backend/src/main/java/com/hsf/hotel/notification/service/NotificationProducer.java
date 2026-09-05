package com.hsf.hotel.notification.service;
import com.hsf.hotel.notification.model.Notification;

import com.hsf.hotel.config.RabbitMQConfig;
import com.hsf.hotel.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendEmailNotification(NotificationEvent event) {
        log.info("Publishing Email Notification Event: type={}, targetId={}", event.getType(), event.getTargetId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "hotel.email." + event.getType(), event);
    }
}
