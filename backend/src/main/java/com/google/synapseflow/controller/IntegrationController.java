package com.google.synapseflow.controller;

import com.google.synapseflow.dto.ApiResponse;
import com.google.synapseflow.dto.IntegrationDto;
import com.google.synapseflow.dto.WorkflowDto;
import com.google.synapseflow.integration.WebhookIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations")
@Tag(name = "Integrations & Webhooks", description = "Endpoints for 3rd-party SaaS webhook ingestion and HMAC verification")
public class IntegrationController {

    private final WebhookIngestionService webhookService;

    public IntegrationController(WebhookIngestionService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhooks/{source}")
    @Operation(summary = "Ingest external webhook from 3rd-party vendor (Workday, Salesforce, Jira, ServiceNow)")
    public ResponseEntity<ApiResponse<WorkflowDto.WorkflowInstanceResponse>> receiveWebhook(
            @PathVariable String source,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody String rawPayload,
            @Valid @RequestBody IntegrationDto.WebhookPayload payload) {

        WorkflowDto.WorkflowInstanceResponse response = webhookService.processIncomingWebhook(
                source,
                rawPayload,
                signatureHeader,
                payload
        );

        return ResponseEntity.ok(ApiResponse.ok("Webhook ingested and workflow triggered successfully", response));
    }
}
