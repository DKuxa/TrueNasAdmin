package org.kuxa.truenasadminworker.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bot")
public record BotProperties(String token, Security security) {

    public record Security(Long adminChatId) {}
}
