package com.opspilot.admin.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_configs")
public class IntegrationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "integration_type", nullable = false)
    private String integrationType;

    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column(name = "auth_type", nullable = false)
    private String authType = "API_KEY";

    @Column(name = "api_key_masked")
    private String apiKeyMasked;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

    @Column(name = "last_test_result")
    private String lastTestResult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public IntegrationConfig() {}

    public IntegrationConfig(UUID organizationId, String name, String integrationType, String endpointUrl, String authType, String apiKeyMasked) {
        this.organizationId = organizationId;
        this.name = name;
        this.integrationType = integrationType;
        this.endpointUrl = endpointUrl;
        this.authType = authType != null ? authType : "API_KEY";
        this.apiKeyMasked = apiKeyMasked;
        this.status = "ACTIVE";
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
