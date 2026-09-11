package com.google.synapseflow.integration;

import com.google.synapseflow.dto.IntegrationDto;
import com.google.synapseflow.entity.DeadLetterEvent;
import com.google.synapseflow.entity.DeadLetterStatus;
import com.google.synapseflow.entity.OutboxEvent;
import com.google.synapseflow.entity.OutboxStatus;
import com.google.synapseflow.exception.ResourceNotFoundException;
import com.google.synapseflow.repository.DeadLetterEventRepository;
import com.google.synapseflow.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeadLetterQueueService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueService.class);

    private final DeadLetterEventRepository dlqRepository;
    private final OutboxEventRepository outboxRepository;

    public DeadLetterQueueService(DeadLetterEventRepository dlqRepository, OutboxEventRepository outboxRepository) {
        this.dlqRepository = dlqRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public DeadLetterEvent quarantineEvent(OutboxEvent outboxEvent, String reason, String stackTrace) {
        log.warn("Quarantining failed outbox event ID {} to DLQ. Reason: {}", outboxEvent.getId(), reason);

        outboxEvent.setStatus(OutboxStatus.DEAD_LETTERED);
        outboxRepository.save(outboxEvent);

        DeadLetterEvent dlq = new DeadLetterEvent(
                outboxEvent.getId(),
                outboxEvent.getAggregateType(),
                outboxEvent.getAggregateId(),
                outboxEvent.getEventType(),
                outboxEvent.getTargetEndpoint(),
                outboxEvent.getPayloadJson(),
                reason,
                stackTrace,
                outboxEvent.getRetryCount()
        );

        return dlqRepository.save(dlq);
    }

    @Transactional(readOnly = true)
    public Page<IntegrationDto.DeadLetterResponse> getQuarantinedEvents(DeadLetterStatus status, Pageable pageable) {
        Page<DeadLetterEvent> page = (status != null)
                ? dlqRepository.findByStatus(status, pageable)
                : dlqRepository.findAll(pageable);

        return page.map(this::mapToDto);
    }

    @Transactional
    public IntegrationDto.ReplayResult replayEvent(Long dlqId, String actorUsername) {
        DeadLetterEvent dlqEvent = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new ResourceNotFoundException("DeadLetterEvent", "id", dlqId));

        if (dlqEvent.getStatus() == DeadLetterStatus.DISCARDED) {
            return new IntegrationDto.ReplayResult(false, "Cannot replay a discarded DLQ event", dlqId, null);
        }

        // Create a fresh OutboxEvent to re-attempt dispatch
        OutboxEvent newOutboxEvent = new OutboxEvent(
                dlqEvent.getAggregateType(),
                dlqEvent.getAggregateId(),
                dlqEvent.getEventType() + ".REPLAY",
                dlqEvent.getTargetEndpoint(),
                dlqEvent.getPayloadJson()
        );
        newOutboxEvent.setStatus(OutboxStatus.PENDING);
        newOutboxEvent.setRetryCount(0);
        newOutboxEvent.setNextRetryAt(Instant.now());
        OutboxEvent saved = outboxRepository.save(newOutboxEvent);

        dlqEvent.setStatus(DeadLetterStatus.REPLAYED);
        dlqEvent.setReplayedAt(Instant.now());
        dlqEvent.setReplayedByUsername(actorUsername);
        dlqRepository.save(dlqEvent);

        log.info("Successfully replayed DLQ event ID {} by actor {}. New Outbox ID: {}", dlqId, actorUsername, saved.getId());
        return new IntegrationDto.ReplayResult(true, "Event re-queued for dispatch successfully", dlqId, saved.getId());
    }

    @Transactional
    public List<IntegrationDto.ReplayResult> replayAllQuarantined(String actorUsername) {
        List<DeadLetterEvent> quarantined = dlqRepository.findAll().stream()
                .filter(e -> e.getStatus() == DeadLetterStatus.QUARANTINED)
                .toList();

        List<IntegrationDto.ReplayResult> results = new ArrayList<>();
        for (DeadLetterEvent dlq : quarantined) {
            results.add(replayEvent(dlq.getId(), actorUsername));
        }
        return results;
    }

    @Transactional
    public void discardEvent(Long dlqId, String actorUsername) {
        DeadLetterEvent dlqEvent = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new ResourceNotFoundException("DeadLetterEvent", "id", dlqId));

        dlqEvent.setStatus(DeadLetterStatus.DISCARDED);
        dlqEvent.setReplayedByUsername(actorUsername);
        dlqRepository.save(dlqEvent);
        log.info("DLQ event ID {} discarded by actor {}", dlqId, actorUsername);
    }

    public long countQuarantined() {
        return dlqRepository.countByStatus(DeadLetterStatus.QUARANTINED);
    }

    private IntegrationDto.DeadLetterResponse mapToDto(DeadLetterEvent dlq) {
        return new IntegrationDto.DeadLetterResponse(
                dlq.getId(),
                dlq.getOriginalOutboxId(),
                dlq.getAggregateType(),
                dlq.getAggregateId(),
                dlq.getEventType(),
                dlq.getTargetEndpoint(),
                dlq.getPayloadJson(),
                dlq.getFailureReason(),
                dlq.getStackTrace(),
                dlq.getRetryAttempts(),
                dlq.getStatus(),
                dlq.getReplayedAt(),
                dlq.getReplayedByUsername(),
                dlq.getCreatedAt()
        );
    }
}
