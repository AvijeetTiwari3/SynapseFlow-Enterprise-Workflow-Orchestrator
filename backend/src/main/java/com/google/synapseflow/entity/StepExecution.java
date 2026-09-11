package com.google.synapseflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "step_executions")
@EntityListeners(AuditingEntityListener.class)
public class StepExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    private WorkflowInstance workflowInstance;

    @Column(nullable = false, length = 100)
    private String stepKey;

    @Column(nullable = false, length = 150)
    private String stepName;

    @Column(nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role requiredRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StepStatus status = StepStatus.PENDING;

    @Column(length = 64)
    private String completedByUsername;

    @Column(columnDefinition = "TEXT")
    private String comments;

    private Instant slaDeadline;

    private Instant completedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public StepExecution() {}

    public StepExecution(String stepKey, String stepName, int stepOrder, Role requiredRole, Instant slaDeadline) {
        this.stepKey = stepKey;
        this.stepName = stepName;
        this.stepOrder = stepOrder;
        this.requiredRole = requiredRole;
        this.slaDeadline = slaDeadline;
        this.status = StepStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowInstance getWorkflowInstance() { return workflowInstance; }
    public void setWorkflowInstance(WorkflowInstance workflowInstance) { this.workflowInstance = workflowInstance; }

    public String getStepKey() { return stepKey; }
    public void setStepKey(String stepKey) { this.stepKey = stepKey; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public Role getRequiredRole() { return requiredRole; }
    public void setRequiredRole(Role requiredRole) { this.requiredRole = requiredRole; }

    public StepStatus getStatus() { return status; }
    public void setStatus(StepStatus status) { this.status = status; }

    public String getCompletedByUsername() { return completedByUsername; }
    public void setCompletedByUsername(String completedByUsername) { this.completedByUsername = completedByUsername; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
