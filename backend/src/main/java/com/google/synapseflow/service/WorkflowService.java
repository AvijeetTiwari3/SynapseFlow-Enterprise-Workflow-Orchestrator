package com.google.synapseflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.synapseflow.audit.AuditService;
import com.google.synapseflow.dto.WorkflowDto;
import com.google.synapseflow.entity.*;
import com.google.synapseflow.exception.AppException;
import com.google.synapseflow.exception.ResourceNotFoundException;
import com.google.synapseflow.fsm.WorkflowEvent;
import com.google.synapseflow.fsm.WorkflowFsmEngine;
import com.google.synapseflow.repository.OutboxEventRepository;
import com.google.synapseflow.repository.StepExecutionRepository;
import com.google.synapseflow.repository.WorkflowInstanceRepository;
import com.google.synapseflow.repository.WorkflowTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final OutboxEventRepository outboxRepository;
    private final WorkflowFsmEngine fsmEngine;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public WorkflowService(
            WorkflowTemplateRepository templateRepository,
            WorkflowInstanceRepository instanceRepository,
            StepExecutionRepository stepExecutionRepository,
            OutboxEventRepository outboxRepository,
            WorkflowFsmEngine fsmEngine,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.instanceRepository = instanceRepository;
        this.stepExecutionRepository = stepExecutionRepository;
        this.outboxRepository = outboxRepository;
        this.fsmEngine = fsmEngine;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    // --- TEMPLATES ---

    @Transactional
    public WorkflowDto.WorkflowTemplateResponse createTemplate(WorkflowDto.CreateTemplateRequest request) {
        if (templateRepository.findByTemplateKey(request.templateKey()).isPresent()) {
            throw new AppException("Template with key '" + request.templateKey() + "' already exists");
        }

        WorkflowTemplate template = new WorkflowTemplate(
                request.templateKey(),
                request.name(),
                request.description(),
                request.category(),
                request.stepsDefinitionJson()
        );

        WorkflowTemplate saved = templateRepository.save(template);
        return mapToTemplateResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkflowDto.WorkflowTemplateResponse> getAllTemplates() {
        return templateRepository.findByActiveTrue().stream()
                .map(this::mapToTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowDto.WorkflowTemplateResponse getTemplateById(Long id) {
        WorkflowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTemplate", "id", id));
        return mapToTemplateResponse(template);
    }

    // --- INSTANCES ---

    @Transactional
    public WorkflowDto.WorkflowInstanceResponse createInstance(WorkflowDto.CreateInstanceRequest request, String initiatorUsername) {
        WorkflowTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTemplate", "id", request.templateId()));

        WorkflowInstance instance = new WorkflowInstance(
                template,
                request.title(),
                initiatorUsername,
                request.contextPayloadJson() != null ? request.contextPayloadJson() : "{}"
        );

        // Parse steps definition from JSON to construct StepExecution records
        try {
            List<Map<String, Object>> stepDefs = objectMapper.readValue(
                    template.getStepsDefinitionJson(),
                    new TypeReference<>() {}
            );

            int order = 1;
            for (Map<String, Object> def : stepDefs) {
                String stepKey = (String) def.getOrDefault("key", "STEP_" + order);
                String stepName = (String) def.getOrDefault("name", "Step " + order);
                String roleStr = (String) def.getOrDefault("requiredRole", "ROLE_APPROVER");
                int slaHours = (int) def.getOrDefault("slaHours", 24);

                Role requiredRole = Role.valueOf(roleStr);
                Instant slaDeadline = Instant.now().plus(slaHours, ChronoUnit.HOURS);

                StepExecution step = new StepExecution(stepKey, stepName, order++, requiredRole, slaDeadline);
                instance.addStepExecution(step);
            }
        } catch (Exception e) {
            log.error("Failed to parse steps definition json for template {}", template.getId(), e);
            throw new AppException("Invalid template steps definition format: " + e.getMessage());
        }

        WorkflowInstance saved = instanceRepository.save(instance);

        auditService.logEvent(
                "WORKFLOW_INSTANCE",
                saved.getId(),
                initiatorUsername,
                "CREATE_WORKFLOW",
                "NONE",
                saved.getCurrentState(),
                request.contextPayloadJson(),
                "127.0.0.1"
        );

        return mapToInstanceResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowDto.WorkflowInstanceResponse> getInstances(WorkflowStatus status, Pageable pageable) {
        Page<WorkflowInstance> page = (status != null)
                ? instanceRepository.findByStatus(status, pageable)
                : instanceRepository.findAll(pageable);

        return page.map(this::mapToInstanceResponse);
    }

    @Transactional(readOnly = true)
    public WorkflowDto.WorkflowInstanceResponse getInstanceById(Long id) {
        WorkflowInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", "id", id));
        return mapToInstanceResponse(instance);
    }

    @Transactional
    public WorkflowDto.WorkflowInstanceResponse transitionWorkflow(
            Long instanceId,
            WorkflowDto.TransitionRequest request,
            String actorUsername) {

        WorkflowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowInstance", "id", instanceId));

        WorkflowEvent event = WorkflowEvent.valueOf(request.event().toUpperCase());

        WorkflowFsmEngine.TransitionOutcome outcome = fsmEngine.processTransition(
                instance,
                event,
                request.stepExecutionId(),
                actorUsername,
                request.note()
        );

        WorkflowInstance updated = instanceRepository.save(instance);

        // Transactional Outbox Pattern: If workflow approved or completed, publish outbound event
        if (outcome.isCompleted() && updated.getStatus() == WorkflowStatus.APPROVED) {
            publishOutboxCompletionEvent(updated);
        }

        auditService.logEvent(
                "WORKFLOW_INSTANCE",
                updated.getId(),
                actorUsername,
                "TRANSITION_" + event.name(),
                outcome.previousState(),
                outcome.newState(),
                request.note(),
                "127.0.0.1"
        );

        return mapToInstanceResponse(updated);
    }

    private void publishOutboxCompletionEvent(WorkflowInstance instance) {
        String payloadJson = String.format("{\"workflowId\":%d,\"templateKey\":\"%s\",\"status\":\"APPROVED\",\"initiator\":\"%s\",\"approvedAt\":\"%s\"}",
                instance.getId(),
                instance.getTemplate().getTemplateKey(),
                instance.getInitiatorUsername(),
                Instant.now().toString());

        OutboxEvent outboxEvent = new OutboxEvent(
                "WORKFLOW_INSTANCE",
                instance.getId(),
                "WORKFLOW.APPROVED",
                "mock://enterprise.google.internal/webhooks/approvals",
                payloadJson
        );

        outboxRepository.save(outboxEvent);
        log.info("Queued Transactional Outbox Event for approved workflow instance {}", instance.getId());
    }

    // SLA Monitoring Background Worker: Polls breached step executions every 60s
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void monitorSlaBreaches() {
        List<StepExecution> breachedSteps = stepExecutionRepository.findBreachedSlaSteps(Instant.now());
        if (breachedSteps.isEmpty()) {
            return;
        }

        log.warn("Found {} step executions breaching SLA deadline. Auto-escalating...", breachedSteps.size());
        for (StepExecution step : breachedSteps) {
            try {
                WorkflowInstance instance = step.getWorkflowInstance();
                if (instance.getStatus() == WorkflowStatus.IN_PROGRESS || instance.getStatus() == WorkflowStatus.PENDING_APPROVAL) {
                    transitionWorkflow(
                            instance.getId(),
                            new WorkflowDto.TransitionRequest("ESCALATE", "Automated SLA Breach Triggered (> SLA Deadline)", step.getId()),
                            "SLA_WATCHDOG_SYSTEM"
                    );
                }
            } catch (Exception e) {
                log.error("Failed to auto-escalate breached step ID {}", step.getId(), e);
            }
        }
    }

    // --- MAPPING HELPERS ---

    private WorkflowDto.WorkflowTemplateResponse mapToTemplateResponse(WorkflowTemplate t) {
        return new WorkflowDto.WorkflowTemplateResponse(
                t.getId(),
                t.getTemplateKey(),
                t.getName(),
                t.getDescription(),
                t.getCategory(),
                t.getStepsDefinitionJson(),
                t.getVersion(),
                t.isActive(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private WorkflowDto.WorkflowInstanceResponse mapToInstanceResponse(WorkflowInstance inst) {
        List<WorkflowDto.StepExecutionResponse> steps = inst.getStepExecutions().stream()
                .map(s -> new WorkflowDto.StepExecutionResponse(
                        s.getId(),
                        s.getStepKey(),
                        s.getStepName(),
                        s.getStepOrder(),
                        s.getRequiredRole(),
                        s.getStatus(),
                        s.getCompletedByUsername(),
                        s.getComments(),
                        s.getSlaDeadline(),
                        s.getCompletedAt(),
                        s.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new WorkflowDto.WorkflowInstanceResponse(
                inst.getId(),
                inst.getTemplate().getId(),
                inst.getTemplate().getTemplateKey(),
                inst.getTemplate().getName(),
                inst.getTemplate().getCategory(),
                inst.getTitle(),
                inst.getInitiatorUsername(),
                inst.getStatus(),
                inst.getCurrentState(),
                inst.getContextPayloadJson(),
                inst.getRejectionReason(),
                inst.getVersion(),
                steps,
                inst.getCreatedAt(),
                inst.getUpdatedAt()
        );
    }
}
