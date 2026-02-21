package org.kuxa.truenasadminworker.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("truenas.api")
public record TrueNasProperties(
        String url,
        String key,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {}
