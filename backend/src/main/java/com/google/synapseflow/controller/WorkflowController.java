package com.google.synapseflow.controller;

import com.google.synapseflow.audit.Auditable;
import com.google.synapseflow.dto.ApiResponse;
import com.google.synapseflow.dto.WorkflowDto;
import com.google.synapseflow.entity.WorkflowStatus;
import com.google.synapseflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow Engine", description = "Endpoints for creating templates, launching instances, and transitioning states")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // --- TEMPLATES ---

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new workflow template (Admin only)")
    @Auditable(action = "CREATE_TEMPLATE", aggregateType = "WORKFLOW_TEMPLATE")
    public ResponseEntity<ApiResponse<WorkflowDto.WorkflowTemplateResponse>> createTemplate(
            @Valid @RequestBody WorkflowDto.CreateTemplateRequest request) {
        WorkflowDto.WorkflowTemplateResponse response = workflowService.createTemplate(request);
        return ResponseEntity.ok(ApiResponse.ok("Template created successfully", response));
    }

    @GetMapping("/templates")
    @Operation(summary = "List all active workflow templates")
    public ResponseEntity<ApiResponse<List<WorkflowDto.WorkflowTemplateResponse>>> getAllTemplates() {
        List<WorkflowDto.WorkflowTemplateResponse> templates = workflowService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @GetMapping("/templates/{id}")
    @Operation(summary = "Get workflow template by ID")
    public ResponseEntity<ApiResponse<WorkflowDto.WorkflowTemplateResponse>> getTemplateById(@PathVariable Long id) {
        WorkflowDto.WorkflowTemplateResponse template = workflowService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.ok(template));
    }

    // --- INSTANCES ---

    @PostMapping("/instances")
    @Operation(summary = "Instantiate a new workflow execution")
    @Auditable(action = "LAUNCH_WORKFLOW", aggregateType = "WORKFLOW_INSTANCE")
    public ResponseEntity<ApiResponse<WorkflowDto.WorkflowInstanceResponse>> createInstance(
            @Valid @RequestBody WorkflowDto.CreateInstanceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        WorkflowDto.WorkflowInstanceResponse response = workflowService.createInstance(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Workflow instance launched successfully", response));
    }

    @GetMapping("/instances")
    @Operation(summary = "List workflow instances with optional status filtering and pagination")
    public ResponseEntity<ApiResponse<Page<WorkflowDto.WorkflowInstanceResponse>>> getInstances(
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<WorkflowDto.WorkflowInstanceResponse> instances = workflowService.getInstances(status, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.ok(instances));
    }

    @GetMapping("/instances/{id}")
    @Operation(summary = "Get detailed state and step execution timeline for a workflow instance")
    public ResponseEntity<ApiResponse<WorkflowDto.WorkflowInstanceResponse>> getInstanceById(@PathVariable Long id) {
        WorkflowDto.WorkflowInstanceResponse response = workflowService.getInstanceById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/instances/{id}/transition")
    @Operation(summary = "Execute state transition / approval / rejection on a workflow instance")
    @Auditable(action = "TRANSITION_WORKFLOW", aggregateType = "WORKFLOW_INSTANCE")
    public ResponseEntity<ApiResponse<WorkflowDto.WorkflowInstanceResponse>> transitionWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowDto.TransitionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        WorkflowDto.WorkflowInstanceResponse response = workflowService.transitionWorkflow(
                id,
                request,
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Workflow transitioned successfully", response));
    }
}
