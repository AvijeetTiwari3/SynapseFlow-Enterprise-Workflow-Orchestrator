package com.google.synapseflow.controller;

import com.google.synapseflow.audit.Auditable;
import com.google.synapseflow.dto.ApiResponse;
import com.google.synapseflow.dto.IntegrationDto;
import com.google.synapseflow.entity.DeadLetterStatus;
import com.google.synapseflow.integration.DeadLetterQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dlq")
@Tag(name = "Dead-Letter Queue (DLQ)", description = "Endpoints for inspecting failed 3rd-party outbox dispatches and triggering replays")
public class DeadLetterQueueController {

    private final DeadLetterQueueService dlqService;

    public DeadLetterQueueController(DeadLetterQueueService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "List quarantined DLQ events with optional status filter")
    public ResponseEntity<ApiResponse<Page<IntegrationDto.DeadLetterResponse>>> getQuarantinedEvents(
            @RequestParam(required = false) DeadLetterStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<IntegrationDto.DeadLetterResponse> events = dlqService.getQuarantinedEvents(
                status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @PostMapping("/events/{id}/replay")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Trigger 1-click replay of a quarantined dead-letter event")
    @Auditable(action = "REPLAY_DLQ_EVENT", aggregateType = "DEAD_LETTER_EVENT")
    public ResponseEntity<ApiResponse<IntegrationDto.ReplayResult>> replayEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        IntegrationDto.ReplayResult result = dlqService.replayEvent(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(result.message(), result));
    }

    @PostMapping("/events/bulk-replay")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk replay all quarantined dead-letter events (Admin only)")
    @Auditable(action = "BULK_REPLAY_DLQ", aggregateType = "DEAD_LETTER_EVENT")
    public ResponseEntity<ApiResponse<List<IntegrationDto.ReplayResult>>> replayAll(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<IntegrationDto.ReplayResult> results = dlqService.replayAllQuarantined(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Bulk replay initiated for " + results.size() + " events", results));
    }

    @PostMapping("/events/{id}/discard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Discard a quarantined DLQ event without replaying")
    @Auditable(action = "DISCARD_DLQ_EVENT", aggregateType = "DEAD_LETTER_EVENT")
    public ResponseEntity<ApiResponse<Void>> discardEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        dlqService.discardEvent(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("DLQ event discarded successfully", null));
    }
}
