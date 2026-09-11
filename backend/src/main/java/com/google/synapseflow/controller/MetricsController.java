package com.google.synapseflow.controller;

import com.google.synapseflow.dto.ApiResponse;
import com.google.synapseflow.dto.DashboardMetricsDto;
import com.google.synapseflow.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Telemetry & Metrics", description = "Endpoints for aggregated system health, SLA tracking, and queue metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregated enterprise operations metrics for dashboard")
    public ResponseEntity<ApiResponse<DashboardMetricsDto>> getSummaryMetrics() {
        DashboardMetricsDto metrics = metricsService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.ok(metrics));
    }
}
