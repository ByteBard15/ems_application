package com.bytebard.core.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.rabbitmq.queues.dl-events}")
    private String dlEventsQueue;

    @Value("${spring.rabbitmq.exchanges.main}")
    private String mainExchange;

    @Value("${spring.rabbitmq.exchanges.dlx}")
    private String dlxExchange;

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host:#{'/'}}")
    private String virtualHost;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setChannelCacheSize(25);
        factory.setChannelCheckoutTimeout(30000);
        factory.setConnectionTimeout(10000);
        factory.setRequestedHeartBeat(60);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        factory.setConnectionNameStrategy(connectionFactory -> "spring-boot-app");

        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, @Qualifier("jsonMessageConverter") MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public Queue eventsQueue() {
        return QueueBuilder.durable(appName)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-message-ttl", 60000)
                .build();
    }

    @Bean
    public FanoutExchange mainExchange() {
        return new FanoutExchange(mainExchange);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(dlxExchange);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlEventsQueue).build();
    }

    @Bean
    public Binding userEventsBinding(@Qualifier("eventsQueue") Queue eventsQueue, @Qualifier("mainExchange") FanoutExchange mainExchange) {
        return BindingBuilder
                .bind(eventsQueue)
                .to(mainExchange);
    }

    @Bean
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue, @Qualifier("dlxExchange") DirectExchange dlxExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(dlxExchange)
                .with("#");
    }
}
