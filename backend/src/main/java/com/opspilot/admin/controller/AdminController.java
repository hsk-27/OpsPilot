package com.opspilot.admin.controller;

import com.opspilot.admin.dto.OrganizationSettingsDto;
import com.opspilot.admin.service.AdminSettingsService;
import com.opspilot.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/settings")
@Tag(name = "Admin Settings", description = "Organization-level business rules, approval thresholds, and tolerances")
public class AdminController {

    private final AdminSettingsService settingsService;

    public AdminController(AdminSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Get organization operational settings")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettings()));
    }

    @PatchMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Update organization operational settings")
    public ResponseEntity<ApiResponse<OrganizationSettingsDto>> updateSettings(
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", settingsService.updateSettings(updates)));
    }
}
