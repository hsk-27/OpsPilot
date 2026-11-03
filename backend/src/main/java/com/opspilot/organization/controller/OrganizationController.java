package com.opspilot.organization.controller;

import com.opspilot.common.response.ApiResponse;
import com.opspilot.organization.dto.InviteMemberRequest;
import com.opspilot.organization.dto.MembershipDto;
import com.opspilot.organization.dto.OrganizationDto;
import com.opspilot.organization.dto.UpdateRoleRequest;
import com.opspilot.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organization Management", description = "Tenant organization profile, settings, and member management")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current organization details")
    public ResponseEntity<ApiResponse<OrganizationDto>> getCurrentOrganization() {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getCurrentOrganization()));
    }

    @PatchMapping("/current")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Update current organization settings")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateCurrentOrganization(
            @RequestBody OrganizationDto update) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.updateCurrentOrganization(update)));
    }

    @GetMapping("/current/members")
    @Operation(summary = "List all members in the current organization")
    public ResponseEntity<ApiResponse<List<MembershipDto>>> getMembers() {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getMembers()));
    }

    @PostMapping("/current/members")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Invite or add a new member to the organization")
    public ResponseEntity<ApiResponse<MembershipDto>> inviteMember(
            @Valid @RequestBody InviteMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Member invited successfully", organizationService.inviteMember(request)));
    }

    @PatchMapping("/current/members/{membershipId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update a member's role")
    public ResponseEntity<ApiResponse<MembershipDto>> updateMemberRole(
            @PathVariable UUID membershipId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", organizationService.updateMemberRole(membershipId, request)));
    }
}
