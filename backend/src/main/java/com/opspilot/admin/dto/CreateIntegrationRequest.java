package com.opspilot.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateIntegrationRequest {

    @NotBlank(message = "Integration name is required")
    private String name;

    @NotBlank(message = "Integration type is required (e.g., ERP_SIMULATOR, ERP_NETSUITE, WEBHOOK)")
    private String integrationType;

    private String endpointUrl;
    private String authType = "API_KEY";
    private String apiKey;

    public CreateIntegrationRequest() {}

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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
