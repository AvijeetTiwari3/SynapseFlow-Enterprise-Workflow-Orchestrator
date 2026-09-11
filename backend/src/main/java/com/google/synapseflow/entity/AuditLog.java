package com.google.synapseflow.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_aggregate", columnList = "aggregateType, aggregateId"),
    @Index(name = "idx_audit_actor", columnList = "actorUsername"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false, length = 64)
    private String actorUsername;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 64)
    private String previousState;

    @Column(length = 64)
    private String newState;

    @Column(columnDefinition = "TEXT")
    private String detailsJson;

    @Column(length = 64)
    private String clientIp;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    public AuditLog() {}

    public AuditLog(String aggregateType, Long aggregateId, String actorUsername, String action,
                    String previousState, String newState, String detailsJson, String clientIp) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.detailsJson = detailsJson;
        this.clientIp = clientIp;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public Long getAggregateId() { return aggregateId; }
    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPreviousState() { return previousState; }
    public void setPreviousState(String previousState) { this.previousState = previousState; }

    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }

    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
