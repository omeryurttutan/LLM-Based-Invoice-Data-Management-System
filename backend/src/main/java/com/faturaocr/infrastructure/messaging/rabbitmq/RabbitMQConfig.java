package com.faturaocr.infrastructure.messaging.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${RABBITMQ_EXTRACTION_EXCHANGE:invoice.extraction}")
    private String extractionExchange;

    @Value("${RABBITMQ_EXTRACTION_QUEUE:invoice.extraction.queue}")
    private String extractionQueue;

    @Value("${RABBITMQ_EXTRACTION_DLQ:invoice.extraction.dlq}")
    private String extractionDlq;

    @Value("${RABBITMQ_RESULT_EXCHANGE:invoice.extraction.result}")
    private String resultExchange;

    @Value("${RABBITMQ_RESULT_QUEUE:invoice.extraction.result.queue}")
    private String resultQueue;

    // Exchanges
    @Bean
    public DirectExchange extractionExchange() {
        return new DirectExchange(extractionExchange, true, false);
    }

    @Bean
    public DirectExchange extractionDlx() {
        return new DirectExchange(extractionExchange + ".dlx", true, false);
    }

    @Bean
    public DirectExchange resultExchange() {
        return new DirectExchange(resultExchange, true, false);
    }

    // Queues
    @Bean
    public Queue extractionQueue() {
        return QueueBuilder.durable(extractionQueue)
                .withArgument("x-dead-letter-exchange", extractionExchange + ".dlx")
                .withArgument("x-dead-letter-routing-key", "extraction.dead")
                .build();
    }

    @Bean
    public Queue extractionDlq() {
        return QueueBuilder.durable(extractionDlq).build();
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(resultQueue).build();
    }

    // Bindings
    @Bean
    public Binding extractionBinding() {
        return BindingBuilder.bind(extractionQueue())
                .to(extractionExchange())
                .with("extraction.request");
    }

    @Bean
    public Binding extractionDlqBinding() {
        return BindingBuilder.bind(extractionDlq())
                .to(extractionDlx())
                .with("extraction.dead");
    }

    @Bean
    public Binding resultBinding() {
        return BindingBuilder.bind(resultQueue())
                .to(resultExchange())
                .with("extraction.result");
    }

    // JSON Message Converter
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
