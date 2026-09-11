package com.google.synapseflow.controller;

import com.google.synapseflow.dto.ApiResponse;
import com.google.synapseflow.dto.AuditDto;
import com.google.synapseflow.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Compliance & Audit", description = "Endpoints for querying the immutable audit ledger and compliance logs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'OPERATOR')")
    @Operation(summary = "Query audit logs with optional aggregateType filtering")
    public ResponseEntity<ApiResponse<Page<AuditDto.AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String aggregateType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditDto.AuditLogResponse> logs = auditService.getAuditLogs(
                aggregateType,
                PageRequest.of(page, size, Sort.by("timestamp").descending())
        );

        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @GetMapping("/timeline/{aggregateType}/{aggregateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'OPERATOR')")
    @Operation(summary = "Get complete chronological audit timeline for a specific entity/workflow")
    public ResponseEntity<ApiResponse<List<AuditDto.AuditLogResponse>>> getTimeline(
            @PathVariable String aggregateType,
            @PathVariable Long aggregateId) {

        List<AuditDto.AuditLogResponse> timeline = auditService.getTimelineForAggregate(aggregateType, aggregateId);
        return ResponseEntity.ok(ApiResponse.ok(timeline));
    }
}
