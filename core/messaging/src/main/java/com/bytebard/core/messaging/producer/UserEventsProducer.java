package com.bytebard.core.messaging.producer;

import com.bytebard.core.messaging.models.Events;
import com.bytebard.core.messaging.models.UserEventMessage;
import com.bytebard.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserEventsProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventsProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.rabbitmq.exchanges.main}")
    private String mainExchange;

    public UserEventsProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendUserCreatedEvent(Long userId) {
        UserEventMessage message = new UserEventMessage(
                userId,
                Events.USER_CREATED,
                DateUtils.now()
        );

        rabbitTemplate.convertAndSend(mainExchange, "", message);
        log.info("Sent user created event: {}", message);
    }
}
