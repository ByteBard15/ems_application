package com.bytebard;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.time.Duration;

public class DockerConfig {
    private final String image;
    private final String tag;
    private final String container;
    private final DockerClient client;

    public DockerConfig(String image, String tag, String container) {
        this.image = image;
        this.tag = tag;
        this.container = container;

        var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        this.client = DockerClientImpl.getInstance(config, httpClient);
    }

    public DockerClient getClient() {
        return client;
    }
}
