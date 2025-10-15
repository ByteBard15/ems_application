package com.bytebard.core.messaging.consumer;

import com.bytebard.core.messaging.models.Events;
import com.bytebard.core.messaging.models.UserEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "spring.rabbitmq.listeners.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@DependsOn("eventsQueue")
public class UserEventsConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserEventsConsumer.class);

    @Value("${spring.application.name}")
    private String appName;

    @RabbitListener(queues = "${spring.application.name}")
    public void handleUserEvent(UserEventMessage message) {
        log.info("{} received user event: {}", appName, message);

        try {
            if (message.eventType().equals(Events.USER_CREATED)) {
                log.info("Processing user created: {}", message.userId());
            } else {
                log.warn("Unknown event type: {}", message.eventType());
            }
        } catch (Exception e) {
            log.error("Error processing user event: {}", message, e);
            throw e;
        }
    }

    @RabbitListener(queues = "${spring.rabbitmq.queues.dl-events}")
    public void handleDeadLetterQueue(Object message) {
        log.error("Message received in Dead Letter Queue: {}", message);
    }
}
