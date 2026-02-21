package org.kuxa.truenasadminworker.worker.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.kuxa.truenasadminworker.worker.config.TrueNasProperties;
import org.kuxa.truenasadminworker.worker.exception.TrueNasApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class TrueNasApiClient {

    private static final Logger log = LoggerFactory.getLogger(TrueNasApiClient.class);

    private final RestClient restClient;

    public TrueNasApiClient(TrueNasProperties props) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.connectTimeoutSeconds()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(props.readTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(props.url() + "/api/v2.0")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.key())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Map<String, String> getAppStatuses() {
        log.debug("Fetching app statuses from TrueNAS");
        try {
            var body = restClient.get()
                    .uri("/app")
                    .retrieve()
                    .body(JsonNode.class);

            Map<String, String> statuses = new HashMap<>();
            if (body != null && body.isArray()) {
                for (var app : body) {
                    statuses.put(app.path("name").asText(), app.path("state").asText());
                }
            }
            log.debug("Fetched {} app statuses", statuses.size());
            return statuses;

        } catch (RestClientResponseException e) {
            throw new TrueNasApiException(
                    "Failed to fetch app statuses: HTTP %d".formatted(e.getStatusCode().value()), e);
        }
    }

    public String controlApp(String appName, String action) {
        log.info("Issuing '{}' command for app '{}'", action, appName);
        try {
            restClient.post()
                    .uri("/app/{action}", action)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("app_name", appName))
                    .retrieve()
                    .toBodilessEntity();

            return "Command `%s` issued for **%s**.".formatted(action, appName);

        } catch (RestClientResponseException e) {
            throw new TrueNasApiException(
                    "Failed to %s app '%s': HTTP %d".formatted(action, appName, e.getStatusCode().value()), e);
        }
    }

    public String controlSystem(String action) {
        log.warn("Issuing system '{}' command", action);
        try {
            restClient.post()
                    .uri("/system/{action}", action)
                    .retrieve()
                    .toBodilessEntity();

            return "System `%s` command accepted by TrueNAS.".formatted(action);

        } catch (RestClientResponseException e) {
            throw new TrueNasApiException(
                    "System command '%s' failed: HTTP %d".formatted(action, e.getStatusCode().value()), e);
        }
    }
}
