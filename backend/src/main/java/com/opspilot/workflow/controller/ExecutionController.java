package com.opspilot.workflow.controller;

import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.workflow.entity.WorkflowExecution;
import com.opspilot.workflow.entity.WorkflowNodeExecution;
import com.opspilot.workflow.repository.WorkflowExecutionRepository;
import com.opspilot.workflow.repository.WorkflowNodeExecutionRepository;
import com.opspilot.workflow.service.WorkflowEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/executions")
@Tag(name = "Workflow Executions", description = "Execution inspection, timeline analysis, cancel, and retry")
public class ExecutionController {

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final WorkflowEngineService workflowEngineService;

    public ExecutionController(
            WorkflowExecutionRepository executionRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            WorkflowEngineService workflowEngineService) {
        this.executionRepository = executionRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.workflowEngineService = workflowEngineService;
    }

    @GetMapping
    @Operation(summary = "List executions for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<WorkflowExecution>>> getExecutions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(executionRepository.findByOrganizationId(orgId, pageable))));
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get execution summary")
    public ResponseEntity<ApiResponse<WorkflowExecution>> getExecution(@PathVariable UUID executionId) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowExecution execution = executionRepository.findByIdAndOrganizationId(executionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", executionId));
        return ResponseEntity.ok(ApiResponse.success(execution));
    }

    @GetMapping("/{executionId}/nodes")
    @Operation(summary = "Get node execution timeline for inspector")
    public ResponseEntity<ApiResponse<List<WorkflowNodeExecution>>> getNodeExecutions(@PathVariable UUID executionId) {
        UUID orgId = TenantContext.getCurrentTenant();
        executionRepository.findByIdAndOrganizationId(executionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", executionId));

        return ResponseEntity.ok(ApiResponse.success(nodeExecutionRepository.findByWorkflowExecutionIdOrderByCreatedAtAsc(executionId)));
    }

    @PostMapping("/{executionId}/cancel")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Cancel a running or queued workflow execution")
    public ResponseEntity<ApiResponse<WorkflowExecution>> cancelExecution(@PathVariable UUID executionId) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowExecution execution = executionRepository.findByIdAndOrganizationId(executionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", executionId));

        workflowEngineService.cancelWorkflowExecution(executionId, "MANUAL_CANCEL_BY_ADMIN");
        execution = executionRepository.findByIdAndOrganizationId(executionId, orgId).orElse(execution);

        return ResponseEntity.ok(ApiResponse.success("Execution cancelled", execution));
    }

    @PostMapping("/{executionId}/retry")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Retry a failed workflow execution from the failed node")
    public ResponseEntity<ApiResponse<WorkflowExecution>> retryExecution(@PathVariable UUID executionId) {
        WorkflowExecution execution = workflowEngineService.retryExecution(executionId);
        return ResponseEntity.ok(ApiResponse.success("Workflow execution retry initiated", execution));
    }

    @PostMapping("/{executionId}/replay")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Replay workflow execution from beginning")
    public ResponseEntity<ApiResponse<WorkflowExecution>> replayExecution(@PathVariable UUID executionId) {
        WorkflowExecution execution = workflowEngineService.replayExecution(executionId);
        return ResponseEntity.ok(ApiResponse.success("Workflow execution replay initiated", execution));
    }
}
