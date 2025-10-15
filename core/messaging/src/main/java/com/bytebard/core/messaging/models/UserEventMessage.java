package com.bytebard.core.messaging.models;

import java.time.LocalDateTime;

public record UserEventMessage(
        Long userId,
        String eventType,
        LocalDateTime timestamp
) {}
