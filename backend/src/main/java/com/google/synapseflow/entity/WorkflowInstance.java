package com.google.synapseflow.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_instances")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", nullable = false)
    private WorkflowTemplate template;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 64)
    private String initiatorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Column(nullable = false, length = 64)
    private String currentState = "START";

    @Column(columnDefinition = "TEXT")
    private String contextPayloadJson;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Optimistic Concurrency Control
    @Version
    private Long version;

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<StepExecution> stepExecutions = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public WorkflowInstance() {}

    public WorkflowInstance(WorkflowTemplate template, String title, String initiatorUsername, String contextPayloadJson) {
        this.template = template;
        this.title = title;
        this.initiatorUsername = initiatorUsername;
        this.contextPayloadJson = contextPayloadJson;
        this.status = WorkflowStatus.DRAFT;
        this.currentState = "DRAFT";
    }

    public void addStepExecution(StepExecution step) {
        stepExecutions.add(step);
        step.setWorkflowInstance(this);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkflowTemplate getTemplate() { return template; }
    public void setTemplate(WorkflowTemplate template) { this.template = template; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInitiatorUsername() { return initiatorUsername; }
    public void setInitiatorUsername(String initiatorUsername) { this.initiatorUsername = initiatorUsername; }

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }

    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }

    public String getContextPayloadJson() { return contextPayloadJson; }
    public void setContextPayloadJson(String contextPayloadJson) { this.contextPayloadJson = contextPayloadJson; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<StepExecution> getStepExecutions() { return stepExecutions; }
    public void setStepExecutions(List<StepExecution> stepExecutions) { this.stepExecutions = stepExecutions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
