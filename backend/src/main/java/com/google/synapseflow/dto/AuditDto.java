package com.google.synapseflow.dto;

import java.time.Instant;

public class AuditDto {

    public record AuditLogResponse(
        Long id,
        String aggregateType,
        Long aggregateId,
        String actorUsername,
        String action,
        String previousState,
        String newState,
        String detailsJson,
        String clientIp,
        Instant timestamp
    ) {}
}
