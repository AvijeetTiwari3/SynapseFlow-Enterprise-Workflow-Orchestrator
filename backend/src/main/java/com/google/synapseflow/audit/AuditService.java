package com.google.synapseflow.audit;

import com.google.synapseflow.dto.AuditDto;
import com.google.synapseflow.entity.AuditLog;
import com.google.synapseflow.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logEvent(String aggregateType, Long aggregateId, String actorUsername, String action,
                             String previousState, String newState, String detailsJson, String clientIp) {
        AuditLog log = new AuditLog(aggregateType, aggregateId, actorUsername, action, previousState, newState, detailsJson, clientIp);
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditDto.AuditLogResponse> getAuditLogs(String aggregateType, Pageable pageable) {
        Page<AuditLog> page = (aggregateType != null && !aggregateType.isBlank())
                ? auditLogRepository.findByAggregateType(aggregateType, pageable)
                : auditLogRepository.findAll(pageable);

        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<AuditDto.AuditLogResponse> getTimelineForAggregate(String aggregateType, Long aggregateId) {
        return auditLogRepository.findByAggregateTypeAndAggregateIdOrderByTimestampDesc(aggregateType, aggregateId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public long countTotalAuditLogs() {
        return auditLogRepository.count();
    }

    private AuditDto.AuditLogResponse mapToDto(AuditLog log) {
        return new AuditDto.AuditLogResponse(
                log.getId(),
                log.getAggregateType(),
                log.getAggregateId(),
                log.getActorUsername(),
                log.getAction(),
                log.getPreviousState(),
                log.getNewState(),
                log.getDetailsJson(),
                log.getClientIp(),
                log.getTimestamp()
        );
    }
}
