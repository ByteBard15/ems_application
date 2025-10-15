package com.bytebard.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;


@Configuration
public class OpenApiRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> openApiRoute() {
        return RouterFunctions
                // Route for /v3/api-docs
                .route(GET("/v3/api-docs"), request -> {
                    try {
                        ClassPathResource resource = new ClassPathResource("static/openapi.yaml");

                        // Read file content
                        byte[] bytes = resource.getInputStream().readAllBytes();
                        String content = new String(bytes, StandardCharsets.UTF_8);

                        return ServerResponse.ok()
                                .contentType(MediaType.parseMediaType("application/vnd.oai.openapi"))
                                .bodyValue(content);

                    } catch (Exception e) {
                        System.err.println("Error loading openapi.yaml: " + e.getMessage());
                        e.printStackTrace();
                        return ServerResponse.notFound().build();
                    }
                })
                // Test route to verify router is working
                .andRoute(GET("/api-docs-test"), request ->
                        ServerResponse.ok()
                                .contentType(MediaType.TEXT_PLAIN)
                                .bodyValue("Router is working!")
                );
    }
}
