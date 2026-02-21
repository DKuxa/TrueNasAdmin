package org.kuxa.truenasadminworker.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("messaging.queues")
public record MessagingProperties(String incoming, String outgoing) {}
