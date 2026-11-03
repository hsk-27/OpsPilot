package com.opspilot.admin.dto;

import com.opspilot.admin.entity.IntegrationConfig;

import java.time.Instant;
import java.util.UUID;

public class IntegrationConfigDto {

    private UUID id;
    private UUID organizationId;
    private String name;
    private String integrationType;
    private String endpointUrl;
    private String authType;
    private String apiKeyMasked;
    private String status;
    private Instant lastTestedAt;
    private String lastTestResult;
    private Instant createdAt;

    public IntegrationConfigDto() {}

    public static IntegrationConfigDto from(IntegrationConfig entity) {
        if (entity == null) return null;
        IntegrationConfigDto dto = new IntegrationConfigDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setName(entity.getName());
        dto.setIntegrationType(entity.getIntegrationType());
        dto.setEndpointUrl(entity.getEndpointUrl());
        dto.setAuthType(entity.getAuthType());
        dto.setApiKeyMasked(entity.getApiKeyMasked());
        dto.setStatus(entity.getStatus());
        dto.setLastTestedAt(entity.getLastTestedAt());
        dto.setLastTestResult(entity.getLastTestResult());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getApiKeyMasked() {
        return apiKeyMasked;
    }

    public void setApiKeyMasked(String apiKeyMasked) {
        this.apiKeyMasked = apiKeyMasked;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastTestedAt() {
        return lastTestedAt;
    }

    public void setLastTestedAt(Instant lastTestedAt) {
        this.lastTestedAt = lastTestedAt;
    }

    public String getLastTestResult() {
        return lastTestResult;
    }

    public void setLastTestResult(String lastTestResult) {
        this.lastTestResult = lastTestResult;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
