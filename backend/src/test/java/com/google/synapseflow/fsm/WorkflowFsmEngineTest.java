package com.google.synapseflow.fsm;

import com.google.synapseflow.entity.*;
import com.google.synapseflow.exception.WorkflowTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowFsmEngineTest {

    private WorkflowFsmEngine fsmEngine;
    private WorkflowInstance instance;

    @BeforeEach
    void setUp() {
        fsmEngine = new WorkflowFsmEngine();

        WorkflowTemplate template = new WorkflowTemplate(
                "TEST_WF",
                "Test Workflow",
                "Test Description",
                "Testing",
                "[]"
        );

        instance = new WorkflowInstance(template, "Test Instance", "test_user", "{}");

        StepExecution step1 = new StepExecution("STEP_1", "Manager Approval", 1, Role.ROLE_APPROVER, Instant.now().plusSeconds(3600));
        StepExecution step2 = new StepExecution("STEP_2", "Director Approval", 2, Role.ROLE_ADMIN, Instant.now().plusSeconds(7200));

        instance.addStepExecution(step1);
        instance.addStepExecution(step2);
    }

    @Test
    @DisplayName("Should successfully submit workflow from DRAFT to IN_PROGRESS")
    void testSubmitWorkflow() {
        WorkflowFsmEngine.TransitionOutcome outcome = fsmEngine.processTransition(
                instance,
                WorkflowEvent.SUBMIT,
                null,
                "test_user",
                "Submitting for review"
        );

        assertEquals("DRAFT", outcome.previousState());
        assertEquals("STEP_1", outcome.newState());
        assertEquals(WorkflowStatus.IN_PROGRESS, instance.getStatus());
        assertFalse(outcome.isCompleted());
    }

    @Test
    @DisplayName("Should progress through multiple approval steps and complete workflow")
    void testSequentialApprovalFlow() {
        // Step 0: Submit
        fsmEngine.processTransition(instance, WorkflowEvent.SUBMIT, null, "test_user", "Submit");

        // Step 1: Approve first step
        StepExecution step1 = instance.getStepExecutions().get(0);
        WorkflowFsmEngine.TransitionOutcome step1Outcome = fsmEngine.processTransition(
                instance,
                WorkflowEvent.APPROVE,
                step1.getId(),
                "manager_jane",
                "Looks good"
        );

        assertEquals(StepStatus.APPROVED, step1.getStatus());
        assertEquals("STEP_2", instance.getCurrentState());
        assertEquals(WorkflowStatus.PENDING_APPROVAL, instance.getStatus());
        assertFalse(step1Outcome.isCompleted());

        // Step 2: Approve final step
        StepExecution step2 = instance.getStepExecutions().get(1);
        WorkflowFsmEngine.TransitionOutcome step2Outcome = fsmEngine.processTransition(
                instance,
                WorkflowEvent.APPROVE,
                step2.getId(),
                "admin",
                "Final executive approval"
        );

        assertEquals(StepStatus.APPROVED, step2.getStatus());
        assertEquals("APPROVED", instance.getCurrentState());
        assertEquals(WorkflowStatus.APPROVED, instance.getStatus());
        assertTrue(step2Outcome.isCompleted());
    }

    @Test
    @DisplayName("Should transition to REJECTED and terminate workflow when rejected")
    void testRejectionFlow() {
        fsmEngine.processTransition(instance, WorkflowEvent.SUBMIT, null, "test_user", "Submit");

        StepExecution step1 = instance.getStepExecutions().get(0);
        WorkflowFsmEngine.TransitionOutcome outcome = fsmEngine.processTransition(
                instance,
                WorkflowEvent.REJECT,
                step1.getId(),
                "manager_jane",
                "Security criteria not met"
        );

        assertEquals(WorkflowStatus.REJECTED, instance.getStatus());
        assertEquals("REJECTED", instance.getCurrentState());
        assertEquals("Security criteria not met", instance.getRejectionReason());
        assertTrue(outcome.isCompleted());
    }

    @Test
    @DisplayName("Should throw WorkflowTransitionException if submitting from non-DRAFT state")
    void testInvalidSubmission() {
        fsmEngine.processTransition(instance, WorkflowEvent.SUBMIT, null, "test_user", "Submit");

        assertThrows(WorkflowTransitionException.class, () ->
                fsmEngine.processTransition(instance, WorkflowEvent.SUBMIT, null, "test_user", "Submit Again")
        );
    }
}
