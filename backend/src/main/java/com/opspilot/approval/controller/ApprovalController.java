package com.opspilot.approval.controller;

import com.opspilot.approval.dto.ApprovalDecisionRequest;
import com.opspilot.approval.dto.ApprovalTaskDto;
import com.opspilot.approval.entity.ApprovalDecision;
import com.opspilot.approval.service.ApprovalService;
import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Human Approvals", description = "Approval task inbox, financial authorizations, and escalation")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/inbox")
    @Operation(summary = "List pending approval tasks for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<ApprovalTaskDto>>> getInbox(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(approvalService.getInbox(pageable)));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get approval task details with associated invoice")
    public ResponseEntity<ApiResponse<ApprovalTaskDto>> getTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(approvalService.getTask(taskId)));
    }

    @PostMapping("/{taskId}/decide")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('APPROVER')")
    @Operation(summary = "Submit an approval decision (APPROVED, REJECTED, CHANGES_REQUESTED)")
    public ResponseEntity<ApiResponse<ApprovalDecision>> decide(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        ApprovalDecision decision = approvalService.decide(taskId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Decision recorded: " + request.getDecision(), decision));
    }
}
