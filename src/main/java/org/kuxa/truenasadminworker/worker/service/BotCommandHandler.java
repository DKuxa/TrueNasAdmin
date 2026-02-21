package org.kuxa.truenasadminworker.worker.service;

import org.kuxa.truenasadminworker.worker.config.BotProperties;
import org.kuxa.truenasadminworker.worker.config.MessagingProperties;
import org.kuxa.truenasadminworker.worker.dto.incoming.BotRequest;
import org.kuxa.truenasadminworker.worker.dto.outgoing.BotResponse;
import org.kuxa.truenasadminworker.worker.exception.TrueNasApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BotCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(BotCommandHandler.class);

    private final Long adminChatId;
    private final String outgoingQueue;
    private final TrueNasApiClient api;
    private final RabbitTemplate rabbitTemplate;

    public BotCommandHandler(BotProperties bot, MessagingProperties messaging,
                             TrueNasApiClient api, RabbitTemplate rabbitTemplate) {
        this.adminChatId = bot.security().adminChatId();
        this.outgoingQueue = messaging.outgoing();
        this.api = api;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "${messaging.queues.incoming}")
    public void handle(BotRequest request) {
        if (request.update() == null
                || request.update().message() == null
                || request.update().message().chat() == null
                || request.update().message().text() == null) {
            log.debug("Received an update without a usable text message — ignoring");
            return;
        }

        Long chatId = request.update().message().chat().id();

        if (!adminChatId.equals(chatId)) {
            log.warn("Unauthorized access attempt from chatId={}", chatId);
            reply(request.botToken(), chatId, "Access Denied.");
            return;
        }

        String[] parts = request.update().message().text().trim().split("\\s+");
        String action = parts[0].toLowerCase();
        log.info("Received command '{}' from chatId={}", action, chatId);

        String responseText;
        try {
            responseText = switch (action) {
                case "/containers" -> formatAppStatuses(api.getAppStatuses());
                case "/start", "/stop", "/restart" ->
                        parts.length > 1
                                ? api.controlApp(parts[1], action.substring(1))
                                : "Usage: `" + action + " <app_name>`";
                case "/restart_server" -> api.controlSystem("reboot");
                case "/shutdown_server" -> api.controlSystem("shutdown");
                default -> """
                        Unknown command. Available commands:
                        /containers — list all apps and their status
                        /start <app> — start an app
                        /stop <app> — stop an app
                        /restart <app> — restart an app
                        /restart_server — reboot the TrueNAS server
                        /shutdown_server — shut down the TrueNAS server""";
            };
        } catch (TrueNasApiException | RestClientException e) {
            log.error("TrueNAS API error while processing command '{}': {}", action, e.getMessage());
            responseText = "TrueNAS error: " + e.getMessage();
        }

        reply(request.botToken(), chatId, responseText);
    }

    private String formatAppStatuses(Map<String, String> statuses) {
        if (statuses.isEmpty()) {
            return "No apps found on TrueNAS.";
        }
        return statuses.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> "• *%s*: `%s`".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private void reply(String token, Long chatId, String text) {
        rabbitTemplate.convertAndSend(outgoingQueue, new BotResponse(token, chatId, text));
    }
}
