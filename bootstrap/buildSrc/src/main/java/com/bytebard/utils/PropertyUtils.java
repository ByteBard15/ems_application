package com.bytebard.utils;

import java.util.Properties;
import java.util.UUID;

public class PropertyUtils {
    public static Properties generateRandomizedProperties() {
        Properties p = new Properties();
        p.setProperty("JWT_SECRET", randomHex(32));
        p.setProperty("JWT_EXPIRATION_IN_HOURS", "24");
        p.setProperty("JWT_ISSUER", "emp-service");

        p.setProperty("DB_PASSWORD", "emp_db");
        p.setProperty("DB_USERNAME", "emp_db");
        p.setProperty("DB_NAME", "emp_db");
        p.setProperty("DB_PORT", "5434");
        p.setProperty("DB_HOST", "localhost");

        p.setProperty("LOG_LEVEL", "INFO");
        p.setProperty("DB_POOL_SIZE", "10");

        p.setProperty("GATEWAY_SERVICE_PORT", "8080");
        p.setProperty("AUTH_SERVICE_PORT", "8081");
        p.setProperty("EMPLOYEE_SERVICE_PORT", "8082");
        p.setProperty("DISCOVERY_SERVICE_PORT", "8761");
        p.setProperty("DEFAULT_PASSWORD", randomHex(16));
        p.setProperty("DISCOVERY_SERVICE_URL", "http://localhost:8761");

        p.setProperty("RABBITMQ_HOST", "localhost");
        p.setProperty("RABBITMQ_PORT", "5672");
        p.setProperty("RABBITMQ_USERNAME", "admin");
        p.setProperty("RABBITMQ_PASSWORD", "admin123");
        p.setProperty("RABBITMQ_VIRTUAL_HOST", "/");

        p.setProperty("RABBITMQ_USER_EVENTS_QUEUE", "user.events.queue");
        p.setProperty("RABBITMQ_DL_EVENTS_QUEUE", "dl.queue");
        p.setProperty("RABBITMQ_MAIN_EXCHANGE", "main.exchange");
        p.setProperty("RABBITMQ_DL_EXCHANGE", "dlx.exchange");
        return p;
    }

    private static String randomHex(int length) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (length <= uuid.length()) return uuid.substring(0, length);
        StringBuilder sb = new StringBuilder(uuid);
        while (sb.length() < length) sb.append(UUID.randomUUID().toString().replace("-", ""));
        return sb.substring(0, length);
    }
}
