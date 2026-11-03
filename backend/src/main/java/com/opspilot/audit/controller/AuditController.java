package com.opspilot.audit.controller;

import com.opspilot.audit.entity.AuditEvent;
import com.opspilot.audit.service.AuditService;
import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit & Compliance", description = "Append-only immutable audit trail and historical logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List audit events for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<AuditEvent>>> getAuditEvents(
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAuditEvents(pageable)));
    }

    @GetMapping("/entity/{entityId}")
    @Operation(summary = "Get audit trail for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> getEntityAuditTrail(@PathVariable String entityId) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAuditTrail(entityId)));
    }
}
