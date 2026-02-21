package org.kuxa.truenasadminworker.worker.dto.incoming;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BotRequest(String botToken, Update update) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Update(
            @JsonProperty("update_id") Long updateId,
            Message message
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("message_id") Long messageId,
            Chat chat,
            String text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(Long id, String type, String username) {}
}
