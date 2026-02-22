package org.kuxa.truenasadminworker.worker.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private final MessagingProperties messaging;

    public RabbitMQConfig(MessagingProperties messaging) {
        this.messaging = messaging;
    }

    @Bean
    public Queue incomingQueue() {
        return new Queue(messaging.incoming(), true);
    }

    private static final String REPLIES_DLQ = "telegram.replies.dlq";

    @Bean
    public Queue outgoingQueue() {
        return QueueBuilder.durable(messaging.outgoing())
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", REPLIES_DLQ)
                .build();
    }

    @Bean
    public Queue repliesDlq() {
        return new Queue(REPLIES_DLQ, true);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        var converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
