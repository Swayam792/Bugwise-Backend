package com.swayam.bugwise.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String IN_APP_QUEUE = "inapp.notifications.queue";
    public static final String IN_APP_EXCHANGE = "inapp.notifications.exchange";
    public static final String IN_APP_ROUTING_KEY = "inapp.notifications.routingKey";

    @Bean
    public Queue inAppQueue() {
        return QueueBuilder.durable(IN_APP_QUEUE)
                .withArgument("x-dead-letter-exchange", "inapp.notifications.dlx")
                .build();
    }

    @Bean
    public TopicExchange inAppExchange() {
        return new TopicExchange(IN_APP_EXCHANGE);
    }

    @Bean
    public Binding inAppBinding(Queue inAppQueue, TopicExchange inAppExchange) {
        return BindingBuilder.bind(inAppQueue)
                .to(inAppExchange)
                .with(IN_APP_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("inapp.notifications.dlx");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("inapp.notifications.dlq").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("inapp.notifications.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setChannelTransacted(true);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }
}