package org.kuxa.truenasadminworker.worker.service;

import org.kuxa.truenasadminworker.worker.config.BotProperties;
import org.kuxa.truenasadminworker.worker.config.MessagingProperties;
import org.kuxa.truenasadminworker.worker.dto.outgoing.BotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StateMonitor {

    private static final Logger log = LoggerFactory.getLogger(StateMonitor.class);

    private final TrueNasApiClient api;
    private final RabbitTemplate rabbitTemplate;
    private final String outgoingQueue;
    private final Long adminChatId;
    private final String botToken;

    private final Map<String, String> previousState = new ConcurrentHashMap<>();

    public StateMonitor(TrueNasApiClient api, RabbitTemplate rabbitTemplate,
                        BotProperties bot, MessagingProperties messaging) {
        this.api = api;
        this.rabbitTemplate = rabbitTemplate;
        this.outgoingQueue = messaging.outgoing();
        this.adminChatId = bot.security().adminChatId();
        this.botToken = bot.token();
    }

    @Scheduled(fixedDelay = 45_000)
    public void monitorApps() {
        log.debug("Running TrueNAS state monitor poll");

        Map<String, String> currentState;
        try {
            currentState = api.getAppStatuses();
        } catch (Exception e) {
            log.error("State monitor failed to reach TrueNAS API: {}", e.getMessage());
            return;
        }

        if (currentState.isEmpty()) {
            log.warn("TrueNAS returned an empty app list — skipping state diff");
            return;
        }

        // First poll: seed baseline; nothing to diff against yet
        if (previousState.isEmpty()) {
            log.info("State monitor initialized with {} apps", currentState.size());
            previousState.putAll(currentState);
            return;
        }

        currentState.forEach((appName, newStatus) -> {
            String oldStatus = previousState.get(appName);
            if (oldStatus != null && !oldStatus.equals(newStatus)) {
                String alert = "Alert: *%s* changed `%s` → `%s`".formatted(appName, oldStatus, newStatus);
                log.warn(alert);
                sendAlert(alert);
            }
        });

        previousState.forEach((appName, oldStatus) -> {
            if (!currentState.containsKey(appName)) {
                String alert = "Alert: *%s* has disappeared from TrueNAS (was `%s`)".formatted(appName, oldStatus);
                log.warn(alert);
                sendAlert(alert);
            }
        });

        previousState.clear();
        previousState.putAll(currentState);
    }

    private void sendAlert(String text) {
        rabbitTemplate.convertAndSend(outgoingQueue, new BotResponse(botToken, adminChatId, text));
    }
}
