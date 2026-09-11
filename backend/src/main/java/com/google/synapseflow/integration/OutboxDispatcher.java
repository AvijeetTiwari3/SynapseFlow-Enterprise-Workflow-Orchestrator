package com.google.synapseflow.integration;

import com.google.synapseflow.entity.OutboxEvent;
import com.google.synapseflow.entity.OutboxStatus;
import com.google.synapseflow.repository.OutboxEventRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository outboxRepository;
    private final DeadLetterQueueService dlqService;
    private final HmacSignatureVerifier signatureVerifier;
    private final CircuitBreaker circuitBreaker;
    private final RestTemplate restTemplate;

    public OutboxDispatcher(
            OutboxEventRepository outboxRepository,
            DeadLetterQueueService dlqService,
            HmacSignatureVerifier signatureVerifier,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.outboxRepository = outboxRepository;
        this.dlqService = dlqService;
        this.signatureVerifier = signatureVerifier;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("thirdPartyIntegration");
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedDelayString = "${app.outbox.polling-interval-ms:3000}")
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEventsToDispatch(
                Instant.now(),
                PageRequest.of(0, 25)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Outbox worker picked up {} pending events for dispatch", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            dispatchEvent(event);
        }
    }

    @Transactional
    public void dispatchEvent(OutboxEvent event) {
        event.setStatus(OutboxStatus.DISPATCHING);
        outboxRepository.save(event);

        try {
            // Execute via Resilience4j Circuit Breaker
            circuitBreaker.executeRunnable(() -> sendHttpRequest(event));

            event.setStatus(OutboxStatus.SENT);
            outboxRepository.save(event);
            log.info("Successfully dispatched Outbox Event ID {} to endpoint {}", event.getId(), event.getTargetEndpoint());

        } catch (Exception ex) {
            handleDispatchFailure(event, ex);
        }
    }

    private void sendHttpRequest(OutboxEvent event) {
        // If targetEndpoint is internal mock/simulation url, succeed immediately
        if (event.getTargetEndpoint().startsWith("mock://") || event.getTargetEndpoint().contains("localhost/mock")) {
            log.info("Simulated delivery successful for target: {}", event.getTargetEndpoint());
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Hub-Signature-256", signatureVerifier.generateSignature(event.getPayloadJson()));
        headers.set("X-SynapseFlow-Event-Id", String.valueOf(event.getId()));
        headers.set("X-SynapseFlow-Event-Type", event.getEventType());

        HttpEntity<String> entity = new HttpEntity<>(event.getPayloadJson(), headers);
        ResponseEntity<String> response = restTemplate.exchange(
                event.getTargetEndpoint(),
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Target endpoint returned non-2xx status: " + response.getStatusCode());
        }
    }

    private void handleDispatchFailure(OutboxEvent event, Exception ex) {
        int currentRetries = event.getRetryCount() + 1;
        event.setRetryCount(currentRetries);
        event.setLastError(ex.getMessage());

        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        if (currentRetries >= event.getMaxRetries()) {
            log.error("Outbox Event ID {} exceeded max retries ({}). Quarantining to DLQ.", event.getId(), event.getMaxRetries());
            dlqService.quarantineEvent(event, "Max retries exceeded: " + ex.getMessage(), stackTrace);
        } else {
            // Exponential backoff with jitter: 2^retry * 2000ms
            long delaySeconds = (long) Math.pow(2, currentRetries) * 2;
            event.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            event.setStatus(OutboxStatus.FAILED);
            outboxRepository.save(event);
            log.warn("Outbox Event ID {} failed. Scheduled retry #{} in {}s. Error: {}",
                    event.getId(), currentRetries, delaySeconds, ex.getMessage());
        }
    }
}
