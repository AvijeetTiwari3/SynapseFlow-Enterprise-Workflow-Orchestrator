package com.google.synapseflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.synapseflow.audit.AuditService;
import com.google.synapseflow.dto.IntegrationDto;
import com.google.synapseflow.dto.WorkflowDto;
import com.google.synapseflow.entity.WorkflowInstance;
import com.google.synapseflow.entity.WorkflowTemplate;
import com.google.synapseflow.exception.AppException;
import com.google.synapseflow.exception.ResourceNotFoundException;
import com.google.synapseflow.exception.UnauthorizedActionException;
import com.google.synapseflow.fsm.WorkflowEvent;
import com.google.synapseflow.fsm.WorkflowFsmEngine;
import com.google.synapseflow.repository.WorkflowInstanceRepository;
import com.google.synapseflow.repository.WorkflowTemplateRepository;
import com.google.synapseflow.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);

    private final HmacSignatureVerifier signatureVerifier;
    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public WebhookIngestionService(
            HmacSignatureVerifier signatureVerifier,
            WorkflowTemplateRepository templateRepository,
            WorkflowService workflowService,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.templateRepository = templateRepository;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowDto.WorkflowInstanceResponse processIncomingWebhook(
            String source,
            String rawPayload,
            String signatureHeader,
            IntegrationDto.WebhookPayload payload) {

        log.info("Received incoming webhook from source={}, eventType={}", source, payload.eventType());

        // Validate HMAC signature if provided (or if production enforce strictly)
        if (signatureHeader != null && !signatureHeader.isBlank()) {
            boolean isValid = signatureVerifier.verifySignature(rawPayload, signatureHeader);
            if (!isValid) {
                log.error("Invalid HMAC-SHA256 signature for webhook from source: {}", source);
                throw new UnauthorizedActionException("Invalid cryptographic webhook signature");
            }
        }

        // Map incoming webhook event into an automated enterprise workflow
        String templateKey = mapSourceToTemplateKey(source, payload.eventType());
        WorkflowTemplate template = templateRepository.findByTemplateKey(templateKey)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTemplate", "templateKey", templateKey));

        String title = String.format("[%s Webhook] %s Event - Auto Triggered",
                source.toUpperCase(), payload.eventType());

        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(payload.data());
        } catch (Exception e) {
            contextJson = "{}";
        }

        WorkflowDto.CreateInstanceRequest request = new WorkflowDto.CreateInstanceRequest(
                template.getId(),
                title,
                contextJson
        );

        WorkflowDto.WorkflowInstanceResponse instance = workflowService.createInstance(request, "WEBHOOK_" + source.toUpperCase());

        // Auto-submit workflow
        workflowService.transitionWorkflow(
                instance.id(),
                new WorkflowDto.TransitionRequest("SUBMIT", "Auto-submitted via " + source + " webhook ingestion", null),
                "WEBHOOK_" + source.toUpperCase()
        );

        auditService.logEvent(
                "WEBHOOK_INGESTION",
                instance.id(),
                "WEBHOOK_" + source.toUpperCase(),
                "INGEST_AND_START_WORKFLOW",
                "RECEIVED",
                "IN_PROGRESS",
                contextJson,
                "0.0.0.0"
        );

        return workflowService.getInstanceById(instance.id());
    }

    private String mapSourceToTemplateKey(String source, String eventType) {
        return switch (source.toLowerCase()) {
            case "workday", "hr" -> "EMPLOYEE_ONBOARDING_V1";
            case "salesforce", "crm" -> "VENDOR_CONTRACT_APPROVAL_V1";
            case "jira", "servicenow" -> "GCP_PROD_ACCESS_V1";
            default -> "GENERIC_BUSINESS_APPROVAL_V1";
        };
    }
}
