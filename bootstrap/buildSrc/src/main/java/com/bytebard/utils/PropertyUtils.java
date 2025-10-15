package com.bytebard.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.UUID;

public class PropertyUtils {
    public static void generateRandomizedProperties(File file, String networkHost, String gitUser, String gitRepo) throws IOException {
        Properties p = new Properties();

        p.setProperty("JWT_SECRET", randomHex(32));
        p.setProperty("JWT_EXPIRATION_IN_HOURS", "24");
        p.setProperty("JWT_ISSUER", "emp-service");

        p.setProperty("DB_PASSWORD", "emp_db");
        p.setProperty("DB_USERNAME", "emp_db");
        p.setProperty("DB_NAME", "emp_db");
        p.setProperty("DB_PORT", "5434");
        p.setProperty("DB_HOST", networkHost);
        p.setProperty("DB_POOL_SIZE", "10");

        p.setProperty("LOG_LEVEL", "INFO");

        p.setProperty("GATEWAY_SERVICE_PORT", "8080");
        p.setProperty("AUTH_SERVICE_PORT", "8081");
        p.setProperty("EMPLOYEE_SERVICE_PORT", "8082");
        p.setProperty("CONFIG_SERVICE_PORT", "8888");
        p.setProperty("DISCOVERY_SERVICE_PORT", "8761");
        p.setProperty("DEFAULT_PASSWORD", "default123");

        p.setProperty("RABBITMQ_HOST", networkHost);
        p.setProperty("RABBITMQ_PORT", "5672");
        p.setProperty("RABBITMQ_USERNAME", "admin");
        p.setProperty("RABBITMQ_PASSWORD", "admin");
        p.setProperty("RABBITMQ_VIRTUAL_HOST", "/");

        p.setProperty("RABBITMQ_USER_EVENTS_QUEUE", "user.events.queue");
        p.setProperty("RABBITMQ_DL_EVENTS_QUEUE", "dl.queue");
        p.setProperty("RABBITMQ_MAIN_EXCHANGE", "main.exchange");
        p.setProperty("RABBITMQ_DL_EXCHANGE", "dlx.exchange");

        p.setProperty("DISCOVERY_SERVICE_URL", "http://" + networkHost + ":8761" + "/eureka");
        p.setProperty("GIT_REPO_LINK", "http://" + networkHost + ":3001" + "/" + gitUser + "/" + gitRepo + ".git");

        p.setProperty("NETWORK_HOST", networkHost);

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("# Generated configuration properties\n");
            for (String name : p.stringPropertyNames()) {
                writer.write(name + "=" + p.getProperty(name) + "\n");
            }
        }
    }

    private static String randomHex(int length) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (length <= uuid.length()) return uuid.substring(0, length);
        StringBuilder sb = new StringBuilder(uuid);
        while (sb.length() < length) sb.append(UUID.randomUUID().toString().replace("-", ""));
        return sb.substring(0, length);
    }
}
