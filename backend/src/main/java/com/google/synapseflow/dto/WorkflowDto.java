package com.google.synapseflow.dto;

import com.google.synapseflow.entity.Role;
import com.google.synapseflow.entity.StepStatus;
import com.google.synapseflow.entity.WorkflowStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class WorkflowDto {

    public record CreateTemplateRequest(
        @NotBlank(message = "Template key is required") String templateKey,
        @NotBlank(message = "Name is required") String name,
        String description,
        @NotBlank(message = "Category is required") String category,
        @NotBlank(message = "Steps definition JSON is required") String stepsDefinitionJson
    ) {}

    public record WorkflowTemplateResponse(
        Long id,
        String templateKey,
        String name,
        String description,
        String category,
        String stepsDefinitionJson,
        int version,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record CreateInstanceRequest(
        @NotNull(message = "Template ID is required") Long templateId,
        @NotBlank(message = "Title is required") String title,
        String contextPayloadJson
    ) {}

    public record TransitionRequest(
        @NotBlank(message = "Event/Action is required (SUBMIT, APPROVE, REJECT, ESCALATE, CANCEL)") String event,
        String note,
        Long stepExecutionId
    ) {}

    public record StepExecutionResponse(
        Long id,
        String stepKey,
        String stepName,
        int stepOrder,
        Role requiredRole,
        StepStatus status,
        String completedByUsername,
        String comments,
        Instant slaDeadline,
        Instant completedAt,
        Instant createdAt
    ) {}

    public record WorkflowInstanceResponse(
        Long id,
        Long templateId,
        String templateKey,
        String templateName,
        String category,
        String title,
        String initiatorUsername,
        WorkflowStatus status,
        String currentState,
        String contextPayloadJson,
        String rejectionReason,
        Long version,
        List<StepExecutionResponse> steps,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
