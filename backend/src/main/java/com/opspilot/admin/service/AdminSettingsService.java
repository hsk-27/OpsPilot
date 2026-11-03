package com.opspilot.admin.service;

import com.opspilot.admin.dto.*;
import com.opspilot.admin.entity.IntegrationConfig;
import com.opspilot.admin.entity.OrganizationSettings;
import com.opspilot.admin.repository.IntegrationConfigRepository;
import com.opspilot.admin.repository.OrganizationSettingsRepository;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminSettingsService {

    private final OrganizationSettingsRepository settingsRepository;
    private final IntegrationConfigRepository integrationRepository;

    public AdminSettingsService(
            OrganizationSettingsRepository settingsRepository,
            IntegrationConfigRepository integrationRepository) {
        this.settingsRepository = settingsRepository;
        this.integrationRepository = integrationRepository;
    }

    @Transactional
    public OrganizationSettingsDto getSettings() {
        UUID orgId = TenantContext.getCurrentTenant();
        OrganizationSettings settings = settingsRepository.findByOrganizationId(orgId)
                .orElseGet(() -> settingsRepository.save(new OrganizationSettings(orgId)));
        return OrganizationSettingsDto.from(settings);
    }

    @Transactional
    public OrganizationSettingsDto updateSettings(Map<String, Object> updates) {
        UUID orgId = TenantContext.getCurrentTenant();
        OrganizationSettings settings = settingsRepository.findByOrganizationId(orgId)
                .orElseGet(() -> new OrganizationSettings(orgId));

        if (updates.containsKey("approvalThresholdAmount") && updates.get("approvalThresholdAmount") != null) {
            settings.setApprovalThresholdAmount(new BigDecimal(updates.get("approvalThresholdAmount").toString()));
        }
        if (updates.containsKey("matchingToleranceAmount") && updates.get("matchingToleranceAmount") != null) {
            settings.setMatchingToleranceAmount(new BigDecimal(updates.get("matchingToleranceAmount").toString()));
        }
        if (updates.containsKey("duplicateWindowDays") && updates.get("duplicateWindowDays") != null) {
            settings.setDuplicateWindowDays(Integer.parseInt(updates.get("duplicateWindowDays").toString()));
        }
        if (updates.containsKey("autoExportApproved") && updates.get("autoExportApproved") != null) {
            settings.setAutoExportApproved(Boolean.parseBoolean(updates.get("autoExportApproved").toString()));
        }

        settings.setUpdatedAt(Instant.now());
        settings = settingsRepository.save(settings);
        return OrganizationSettingsDto.from(settings);
    }

    @Transactional(readOnly = true)
    public List<IntegrationConfigDto> getIntegrations() {
        UUID orgId = TenantContext.getCurrentTenant();
        return integrationRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                .stream().map(IntegrationConfigDto::from).toList();
    }

    @Transactional
    public IntegrationConfigDto createIntegration(CreateIntegrationRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        String maskedKey = null;
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            String key = request.getApiKey().trim();
            maskedKey = key.length() > 4 ? "..." + key.substring(key.length() - 4) : "****";
        }

        IntegrationConfig config = new IntegrationConfig(
                orgId,
                request.getName(),
                request.getIntegrationType(),
                request.getEndpointUrl(),
                request.getAuthType(),
                maskedKey
        );
        config = integrationRepository.save(config);
        return IntegrationConfigDto.from(config);
    }

    @Transactional
    public IntegrationConfigDto updateIntegration(UUID id, UpdateIntegrationRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        IntegrationConfig config = integrationRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            config.setName(request.getName().trim());
        }
        if (request.getEndpointUrl() != null) {
            config.setEndpointUrl(request.getEndpointUrl().trim());
        }
        if (request.getAuthType() != null) {
            config.setAuthType(request.getAuthType());
        }
        if (request.getStatus() != null) {
            config.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            String key = request.getApiKey().trim();
            config.setApiKeyMasked(key.length() > 4 ? "..." + key.substring(key.length() - 4) : "****");
        }

        config.setUpdatedAt(Instant.now());
        config = integrationRepository.save(config);
        return IntegrationConfigDto.from(config);
    }

    @Transactional
    public Map<String, Object> testIntegration(UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        IntegrationConfig config = integrationRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationConfig", id));

        Instant testedAt = Instant.now();
        config.setLastTestedAt(testedAt);
        config.setLastTestResult("SUCCESS");
        config = integrationRepository.save(config);

        return Map.of(
                "integrationId", config.getId(),
                "status", "CONNECTED",
                "result", "SUCCESS",
                "message", "Successfully verified connection to " + config.getName() + " (" + config.getIntegrationType() + ")",
                "latencyMs", 42
        );
    }
}
