package com.google.synapseflow.dto;

import com.google.synapseflow.entity.DeadLetterStatus;
import com.google.synapseflow.entity.OutboxStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

public class IntegrationDto {

    public record WebhookPayload(
        @NotBlank(message = "Source is required") String source,
        @NotBlank(message = "Event type is required") String eventType,
        Map<String, Object> data,
        String signature
    ) {}

    public record OutboxResponse(
        Long id,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String targetEndpoint,
        String payloadJson,
        OutboxStatus status,
        int retryCount,
        int maxRetries,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt
    ) {}

    public record DeadLetterResponse(
        Long id,
        Long originalOutboxId,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String targetEndpoint,
        String payloadJson,
        String failureReason,
        String stackTrace,
        int retryAttempts,
        DeadLetterStatus status,
        Instant replayedAt,
        String replayedByUsername,
        Instant createdAt
    ) {}

    public record ReplayResult(
        boolean success,
        String message,
        Long deadLetterId,
        Long newOutboxId
    ) {}
}
