package com.google.synapseflow.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "dead_letter_events", indexes = {
    @Index(name = "idx_dlq_status", columnList = "status"),
    @Index(name = "idx_dlq_created", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long originalOutboxId;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 500)
    private String targetEndpoint;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    private int retryAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeadLetterStatus status = DeadLetterStatus.QUARANTINED;

    private Instant replayedAt;

    @Column(length = 64)
    private String replayedByUsername;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public DeadLetterEvent() {}

    public DeadLetterEvent(Long originalOutboxId, String aggregateType, Long aggregateId, String eventType,
                           String targetEndpoint, String payloadJson, String failureReason, String stackTrace,
                           int retryAttempts) {
        this.originalOutboxId = originalOutboxId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.targetEndpoint = targetEndpoint;
        this.payloadJson = payloadJson;
        this.failureReason = failureReason;
        this.stackTrace = stackTrace;
        this.retryAttempts = retryAttempts;
        this.status = DeadLetterStatus.QUARANTINED;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOriginalOutboxId() { return originalOutboxId; }
    public void setOriginalOutboxId(Long originalOutboxId) { this.originalOutboxId = originalOutboxId; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public Long getAggregateId() { return aggregateId; }
    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTargetEndpoint() { return targetEndpoint; }
    public void setTargetEndpoint(String targetEndpoint) { this.targetEndpoint = targetEndpoint; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public int getRetryAttempts() { return retryAttempts; }
    public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }

    public DeadLetterStatus getStatus() { return status; }
    public void setStatus(DeadLetterStatus status) { this.status = status; }

    public Instant getReplayedAt() { return replayedAt; }
    public void setReplayedAt(Instant replayedAt) { this.replayedAt = replayedAt; }

    public String getReplayedByUsername() { return replayedByUsername; }
    public void setReplayedByUsername(String replayedByUsername) { this.replayedByUsername = replayedByUsername; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
