package com.google.synapseflow.dto;

import java.util.Map;

public record DashboardMetricsDto(
    long totalWorkflows,
    long activeWorkflows,
    long approvedWorkflows,
    long rejectedWorkflows,
    long pendingApprovals,
    long outboxPending,
    long outboxSent,
    long dlqQuarantined,
    long auditLogCount,
    Map<String, Long> statusDistribution,
    double systemSuccessRate
) {}
