package com.google.synapseflow.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "workflow_templates")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stepsDefinitionJson;

    private int version = 1;

    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public WorkflowTemplate() {}

    public WorkflowTemplate(String templateKey, String name, String description, String category, String stepsDefinitionJson) {
        this.templateKey = templateKey;
        this.name = name;
        this.description = description;
        this.category = category;
        this.stepsDefinitionJson = stepsDefinitionJson;
        this.version = 1;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStepsDefinitionJson() { return stepsDefinitionJson; }
    public void setStepsDefinitionJson(String stepsDefinitionJson) { this.stepsDefinitionJson = stepsDefinitionJson; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
