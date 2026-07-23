package com.fx.api.feed;

import com.fx.api.feed.model.Ack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Our outbound half of the contract: tell fx-orchestrator what we did with a batch.
 *
 * The URL is configuration, not a constant — it is localhost:8081 when you run the app
 * from your IDE and http://fx-orchestrator:8080 inside compose. Same jar, same code.
 */
@Component
public class OrchestratorClient {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorClient.class);

    private final RestTemplate http;
    private final String baseUrl;

    public OrchestratorClient(RestTemplate http, @Value("${fx.orchestrator.url}") String baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public void ack(long batchId, String status) {
        try {
            http.postForEntity(baseUrl + "/api/feed/ack", new Ack(batchId, status), Void.class);
            log.info("ACK batch {} -> {}", batchId, status);
        } catch (RestClientException e) {
            // Never let a failed callback break our own request handling.
            log.warn("Could not ACK batch {} at {}: {}", batchId, baseUrl, e.getMessage());
        }
    }
}
