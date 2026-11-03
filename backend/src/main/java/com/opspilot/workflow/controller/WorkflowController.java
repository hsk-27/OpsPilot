package com.opspilot.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.security.UserPrincipal;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.workflow.entity.WorkflowDefinition;
import com.opspilot.workflow.entity.WorkflowVersion;
import com.opspilot.workflow.model.WorkflowGraph;
import com.opspilot.workflow.repository.WorkflowDefinitionRepository;
import com.opspilot.workflow.repository.WorkflowVersionRepository;
import com.opspilot.workflow.service.WorkflowValidationService;
import com.opspilot.workflow.entity.WorkflowExecution;
import com.opspilot.workflow.service.WorkflowEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows", description = "Workflow visual graph construction, validation, publishing, and versioning")
public class WorkflowController {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowValidationService validationService;
    private final WorkflowEngineService workflowEngineService;
    private final ObjectMapper objectMapper;

    public WorkflowController(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowVersionRepository versionRepository,
            WorkflowValidationService validationService,
            WorkflowEngineService workflowEngineService,
            ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.validationService = validationService;
        this.workflowEngineService = workflowEngineService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "List workflows for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowDefinition>>> getWorkflows(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(definitionRepository.findByOrganizationId(orgId, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow definition and active graph")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWorkflow(@PathVariable UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowDefinition def = definitionRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", id));

        List<WorkflowVersion> versions = versionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(def.getId());
        WorkflowVersion latestVersion = versions.isEmpty() ? null : versions.get(0);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "definition", def,
                "latestVersion", latestVersion != null ? latestVersion : Map.of()
        )));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Create new workflow definition draft")
    public ResponseEntity<ApiResponse<WorkflowDefinition>> createWorkflow(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = TenantContext.getCurrentTenant();
        String name = body.getOrDefault("name", "New Pipeline");
        String desc = body.getOrDefault("description", "");

        WorkflowDefinition def = new WorkflowDefinition(orgId, name, desc, principal.getId());
        def = definitionRepository.save(def);
        return ResponseEntity.ok(ApiResponse.success("Workflow draft created", def));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate workflow graph topology and node rules")
    public ResponseEntity<ApiResponse<WorkflowValidationService.ValidationReport>> validateGraph(
            @PathVariable UUID id,
            @RequestBody WorkflowGraph graph) {
        return ResponseEntity.ok(ApiResponse.success(validationService.validate(graph)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Publish immutable workflow version")
    public ResponseEntity<ApiResponse<WorkflowVersion>> publishWorkflow(
            @PathVariable UUID id,
            @RequestBody WorkflowGraph graph,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowDefinition def = definitionRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", id));

        WorkflowValidationService.ValidationReport report = validationService.validate(graph);
        if (!report.isValid()) {
            throw new ApiException("Cannot publish invalid workflow: " + String.join(", ", report.getErrors()),
                    HttpStatus.BAD_REQUEST, "INVALID_WORKFLOW_GRAPH");
        }

        try {
            String graphJson = objectMapper.writeValueAsString(graph);
            String reportJson = objectMapper.writeValueAsString(report);

            Integer maxVer = versionRepository.findMaxVersionNumber(def.getId());
            int newVersionNum = (maxVer != null ? maxVer : 0) + 1;

            WorkflowVersion version = new WorkflowVersion(def.getId(), newVersionNum, graphJson, principal.getId());
            version.setValidationResultJson(reportJson);
            version = versionRepository.save(version);

            def.setStatus("ACTIVE");
            definitionRepository.save(def);

            return ResponseEntity.ok(ApiResponse.success("Workflow version published successfully", version));

        } catch (Exception e) {
            throw new ApiException("Failed to publish workflow version: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "PUBLISH_ERROR");
        }
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get published version history for a workflow")
    public ResponseEntity<ApiResponse<List<WorkflowVersion>>> getVersions(@PathVariable UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowDefinition def = definitionRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", id));
        return ResponseEntity.ok(ApiResponse.success(versionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(def.getId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Update workflow definition details (name, description, status)")
    public ResponseEntity<ApiResponse<WorkflowDefinition>> updateWorkflow(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowDefinition def = definitionRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", id));

        if (body.containsKey("name")) def.setName(body.get("name"));
        if (body.containsKey("description")) def.setDescription(body.get("description"));
        if (body.containsKey("status")) def.setStatus(body.get("status"));

        def = definitionRepository.save(def);
        return ResponseEntity.ok(ApiResponse.success("Workflow updated successfully", def));
    }

    @PostMapping("/{id}/rollback/{versionId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Rollback workflow definition to a prior published version")
    public ResponseEntity<ApiResponse<WorkflowVersion>> rollbackWorkflow(
            @PathVariable UUID id,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowDefinition def = definitionRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", id));

        WorkflowVersion targetVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowVersion", versionId));

        if (!targetVersion.getWorkflowDefinitionId().equals(def.getId())) {
            throw new ApiException("Version does not belong to workflow", HttpStatus.BAD_REQUEST, "INVALID_VERSION");
        }

        Integer maxVer = versionRepository.findMaxVersionNumber(def.getId());
        int newVersionNum = (maxVer != null ? maxVer : 0) + 1;

        WorkflowVersion rolledBackVersion = new WorkflowVersion(def.getId(), newVersionNum, targetVersion.getGraphDefinitionJson(), principal.getId());
        rolledBackVersion.setValidationResultJson(targetVersion.getValidationResultJson());
        rolledBackVersion = versionRepository.save(rolledBackVersion);

        def.setStatus("ACTIVE");
        definitionRepository.save(def);

        return ResponseEntity.ok(ApiResponse.success("Workflow rolled back to version " + targetVersion.getVersionNumber() + " as v" + newVersionNum, rolledBackVersion));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Manually execute workflow on an invoice")
    public ResponseEntity<ApiResponse<WorkflowExecution>> executeWorkflow(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String invoiceIdStr = body.get("invoiceId");
        if (invoiceIdStr == null || invoiceIdStr.isBlank()) {
            throw new ApiException("invoiceId is required", HttpStatus.BAD_REQUEST, "MISSING_INVOICE_ID");
        }
        UUID invoiceId = UUID.fromString(invoiceIdStr.trim());
        WorkflowExecution execution = workflowEngineService.startWorkflow(id, invoiceId);
        return ResponseEntity.ok(ApiResponse.success("Workflow execution started", execution));
    }
}
