package com.google.synapseflow.service;

import com.google.synapseflow.dto.DashboardMetricsDto;
import com.google.synapseflow.entity.DeadLetterStatus;
import com.google.synapseflow.entity.OutboxStatus;
import com.google.synapseflow.entity.WorkflowStatus;
import com.google.synapseflow.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private final WorkflowInstanceRepository instanceRepository;
    private final OutboxEventRepository outboxRepository;
    private final DeadLetterEventRepository dlqRepository;
    private final AuditLogRepository auditLogRepository;

    public MetricsService(
            WorkflowInstanceRepository instanceRepository,
            OutboxEventRepository outboxRepository,
            DeadLetterEventRepository dlqRepository,
            AuditLogRepository auditLogRepository) {
        this.instanceRepository = instanceRepository;
        this.outboxRepository = outboxRepository;
        this.dlqRepository = dlqRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics() {
        long totalWorkflows = instanceRepository.count();
        long approvedWorkflows = instanceRepository.countByStatus(WorkflowStatus.APPROVED);
        long rejectedWorkflows = instanceRepository.countByStatus(WorkflowStatus.REJECTED);
        long activeWorkflows = instanceRepository.countByStatus(WorkflowStatus.IN_PROGRESS);
        long pendingApprovals = instanceRepository.countByStatus(WorkflowStatus.PENDING_APPROVAL);

        long outboxPending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long outboxSent = outboxRepository.countByStatus(OutboxStatus.SENT);
        long dlqQuarantined = dlqRepository.countByStatus(DeadLetterStatus.QUARANTINED);
        long auditLogCount = auditLogRepository.count();

        Map<String, Long> statusDistribution = new HashMap<>();
        List<Object[]> statusCounts = instanceRepository.countGroupedByStatus();
        for (Object[] row : statusCounts) {
            WorkflowStatus status = (WorkflowStatus) row[0];
            Long count = (Long) row[1];
            statusDistribution.put(status.name(), count);
        }

        double successRate = totalWorkflows > 0
                ? ((double) approvedWorkflows / (double) (totalWorkflows)) * 100.0
                : 100.0;

        return new DashboardMetricsDto(
                totalWorkflows,
                activeWorkflows,
                approvedWorkflows,
                rejectedWorkflows,
                pendingApprovals,
                outboxPending,
                outboxSent,
                dlqQuarantined,
                auditLogCount,
                statusDistribution,
                Math.round(successRate * 10.0) / 10.0
        );
    }
}
