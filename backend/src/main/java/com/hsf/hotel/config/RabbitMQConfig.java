package com.hsf.hotel.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "hotel.exchange";
    
    public static final String EMAIL_QUEUE = "hotel.email.queue";
    public static final String WEBHOOK_QUEUE = "hotel.webhook.queue";

    public static final String EMAIL_ROUTING_KEY = "hotel.email.#";
    public static final String WEBHOOK_ROUTING_KEY = "hotel.webhook.#";

    @Bean
    public TopicExchange hotelExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true); // Durable queue
    }

    @Bean
    public Queue webhookQueue() {
        return new Queue(WEBHOOK_QUEUE, true);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange hotelExchange) {
        return BindingBuilder.bind(emailQueue).to(hotelExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding webhookBinding(Queue webhookQueue, TopicExchange hotelExchange) {
        return BindingBuilder.bind(webhookQueue).to(hotelExchange).with(WEBHOOK_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
