package org.kuxa.truenasadminworker.worker.dto.outgoing;

public record BotResponse(String botToken, Long chatId, String text) {}
