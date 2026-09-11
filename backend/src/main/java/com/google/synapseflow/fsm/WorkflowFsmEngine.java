package com.google.synapseflow.fsm;

import com.google.synapseflow.entity.StepExecution;
import com.google.synapseflow.entity.StepStatus;
import com.google.synapseflow.entity.WorkflowInstance;
import com.google.synapseflow.entity.WorkflowStatus;
import com.google.synapseflow.exception.WorkflowTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class WorkflowFsmEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowFsmEngine.class);

    public record TransitionOutcome(
            String previousState,
            String newState,
            WorkflowStatus previousStatus,
            WorkflowStatus newStatus,
            boolean isCompleted
    ) {}

    public TransitionOutcome processTransition(
            WorkflowInstance instance,
            WorkflowEvent event,
            Long stepExecutionId,
            String actorUsername,
            String note) {

        String prevState = instance.getCurrentState();
        WorkflowStatus prevStatus = instance.getStatus();

        log.info("Processing FSM transition: instanceId={}, event={}, currentState={}, currentStatus={}, actor={}",
                instance.getId(), event, prevState, prevStatus, actorUsername);

        switch (event) {
            case SUBMIT -> handleSubmission(instance);
            case APPROVE -> handleApproval(instance, stepExecutionId, actorUsername, note);
            case REJECT -> handleRejection(instance, stepExecutionId, actorUsername, note);
            case ESCALATE -> handleEscalation(instance, stepExecutionId, actorUsername, note);
            case CANCEL -> handleCancellation(instance, actorUsername, note);
            case COMPLETE -> handleCompletion(instance);
            default -> throw new WorkflowTransitionException("Unsupported workflow event: " + event);
        }

        return new TransitionOutcome(
                prevState,
                instance.getCurrentState(),
                prevStatus,
                instance.getStatus(),
                instance.getStatus() == WorkflowStatus.APPROVED ||
                instance.getStatus() == WorkflowStatus.COMPLETED ||
                instance.getStatus() == WorkflowStatus.REJECTED ||
                instance.getStatus() == WorkflowStatus.CANCELLED
        );
    }

    private void handleSubmission(WorkflowInstance instance) {
        if (instance.getStatus() != WorkflowStatus.DRAFT) {
            throw new WorkflowTransitionException("Workflow can only be submitted from DRAFT status. Current: " + instance.getStatus());
        }

        instance.setStatus(WorkflowStatus.IN_PROGRESS);
        instance.setCurrentState("STEP_1_PENDING");

        List<StepExecution> steps = instance.getStepExecutions();
        if (!steps.isEmpty()) {
            StepExecution firstStep = steps.get(0);
            firstStep.setStatus(StepStatus.PENDING);
            instance.setCurrentState(firstStep.getStepKey());
        }
    }

    private void handleApproval(WorkflowInstance instance, Long stepExecutionId, String actorUsername, String note) {
        if (instance.getStatus() != WorkflowStatus.IN_PROGRESS && instance.getStatus() != WorkflowStatus.PENDING_APPROVAL) {
            throw new WorkflowTransitionException("Cannot approve workflow in status: " + instance.getStatus());
        }

        List<StepExecution> steps = instance.getStepExecutions();
        StepExecution targetStep = null;

        if (stepExecutionId != null) {
            targetStep = steps.stream()
                    .filter(s -> s.getId().equals(stepExecutionId))
                    .findFirst()
                    .orElseThrow(() -> new WorkflowTransitionException("Step execution not found with ID: " + stepExecutionId));
        } else {
            targetStep = steps.stream()
                    .filter(s -> s.getStatus() == StepStatus.PENDING)
                    .findFirst()
                    .orElseThrow(() -> new WorkflowTransitionException("No pending step available to approve"));
        }

        targetStep.setStatus(StepStatus.APPROVED);
        targetStep.setCompletedByUsername(actorUsername);
        targetStep.setComments(note);
        targetStep.setCompletedAt(Instant.now());

        // Check if there are remaining pending steps
        boolean hasRemainingSteps = steps.stream().anyMatch(s -> s.getStatus() == StepStatus.PENDING);

        if (!hasRemainingSteps) {
            instance.setStatus(WorkflowStatus.APPROVED);
            instance.setCurrentState("APPROVED");
        } else {
            StepExecution nextStep = steps.stream()
                    .filter(s -> s.getStatus() == StepStatus.PENDING)
                    .findFirst()
                    .orElse(null);

            if (nextStep != null) {
                instance.setCurrentState(nextStep.getStepKey());
                instance.setStatus(WorkflowStatus.PENDING_APPROVAL);
            }
        }
    }

    private void handleRejection(WorkflowInstance instance, Long stepExecutionId, String actorUsername, String reason) {
        if (instance.getStatus() == WorkflowStatus.APPROVED || instance.getStatus() == WorkflowStatus.COMPLETED) {
            throw new WorkflowTransitionException("Cannot reject an already completed/approved workflow");
        }

        List<StepExecution> steps = instance.getStepExecutions();
        if (stepExecutionId != null) {
            steps.stream()
                    .filter(s -> s.getId().equals(stepExecutionId))
                    .findFirst()
                    .ifPresent(s -> {
                        s.setStatus(StepStatus.REJECTED);
                        s.setCompletedByUsername(actorUsername);
                        s.setComments(reason);
                        s.setCompletedAt(Instant.now());
                    });
        }

        instance.setStatus(WorkflowStatus.REJECTED);
        instance.setCurrentState("REJECTED");
        instance.setRejectionReason(reason != null ? reason : "Rejected by " + actorUsername);
    }

    private void handleEscalation(WorkflowInstance instance, Long stepExecutionId, String actorUsername, String note) {
        instance.setStatus(WorkflowStatus.ESCALATED);
        instance.setCurrentState("ESCALATED_SLA_BREACH");

        if (stepExecutionId != null) {
            instance.getStepExecutions().stream()
                    .filter(s -> s.getId().equals(stepExecutionId))
                    .findFirst()
                    .ifPresent(s -> {
                        s.setStatus(StepStatus.ESCALATED);
                        s.setComments("Escalated: " + (note != null ? note : "SLA Timeout threshold breached"));
                    });
        }
    }

    private void handleCancellation(WorkflowInstance instance, String actorUsername, String note) {
        if (instance.getStatus() == WorkflowStatus.APPROVED || instance.getStatus() == WorkflowStatus.COMPLETED) {
            throw new WorkflowTransitionException("Cannot cancel an already completed workflow");
        }
        instance.setStatus(WorkflowStatus.CANCELLED);
        instance.setCurrentState("CANCELLED");
        instance.setRejectionReason("Cancelled by " + actorUsername + (note != null ? ": " + note : ""));
    }

    private void handleCompletion(WorkflowInstance instance) {
        instance.setStatus(WorkflowStatus.COMPLETED);
        instance.setCurrentState("COMPLETED");
    }
}
