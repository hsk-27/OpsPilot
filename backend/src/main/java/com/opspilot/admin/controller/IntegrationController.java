package com.opspilot.admin.controller;

import com.opspilot.admin.dto.CreateIntegrationRequest;
import com.opspilot.admin.dto.IntegrationConfigDto;
import com.opspilot.admin.dto.UpdateIntegrationRequest;
import com.opspilot.admin.service.AdminSettingsService;
import com.opspilot.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations")
@Tag(name = "Integrations", description = "External ERP, accounting systems, and webhook configurations")
public class IntegrationController {

    private final AdminSettingsService settingsService;

    public IntegrationController(AdminSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "List all external integrations for organization")
    public ResponseEntity<ApiResponse<List<IntegrationConfigDto>>> getIntegrations() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getIntegrations()));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Create an external integration connection")
    public ResponseEntity<ApiResponse<IntegrationConfigDto>> createIntegration(
            @Valid @RequestBody CreateIntegrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Integration created successfully", settingsService.createIntegration(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Update an external integration")
    public ResponseEntity<ApiResponse<IntegrationConfigDto>> updateIntegration(
            @PathVariable UUID id,
            @RequestBody UpdateIntegrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Integration updated successfully", settingsService.updateIntegration(id, request)));
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Test connectivity with external integration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testIntegration(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Connectivity test completed", settingsService.testIntegration(id)));
    }
}
