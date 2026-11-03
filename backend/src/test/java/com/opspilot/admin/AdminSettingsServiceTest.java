package com.opspilot.admin;

import com.opspilot.admin.dto.*;
import com.opspilot.admin.entity.IntegrationConfig;
import com.opspilot.admin.entity.OrganizationSettings;
import com.opspilot.admin.repository.IntegrationConfigRepository;
import com.opspilot.admin.repository.OrganizationSettingsRepository;
import com.opspilot.admin.service.AdminSettingsService;
import com.opspilot.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AdminSettingsServiceTest {

    private AdminSettingsService adminSettingsService;
    private final UUID tenantId = UUID.randomUUID();
    private final Map<UUID, OrganizationSettings> settingsStore = new HashMap<>();
    private final Map<UUID, IntegrationConfig> integrationStore = new HashMap<>();

    @BeforeEach
    public void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        settingsStore.clear();
        integrationStore.clear();

        OrganizationSettingsRepository settingsRepo = (OrganizationSettingsRepository) Proxy.newProxyInstance(
                OrganizationSettingsRepository.class.getClassLoader(),
                new Class<?>[]{OrganizationSettingsRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByOrganizationId".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        return settingsStore.values().stream()
                                .filter(s -> orgId.equals(s.getOrganizationId()))
                                .findFirst();
                    }
                    if ("save".equals(name)) {
                        OrganizationSettings s = (OrganizationSettings) args[0];
                        if (s.getId() == null) s.setId(UUID.randomUUID());
                        settingsStore.put(s.getId(), s);
                        return s;
                    }
                    return null;
                }
        );

        IntegrationConfigRepository integrationRepo = (IntegrationConfigRepository) Proxy.newProxyInstance(
                IntegrationConfigRepository.class.getClassLoader(),
                new Class<?>[]{IntegrationConfigRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByOrganizationIdOrderByCreatedAtDesc".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        return integrationStore.values().stream()
                                .filter(i -> orgId.equals(i.getOrganizationId()))
                                .sorted(Comparator.comparing(IntegrationConfig::getCreatedAt).reversed())
                                .toList();
                    }
                    if ("findByIdAndOrganizationId".equals(name)) {
                        UUID id = (UUID) args[0];
                        UUID orgId = (UUID) args[1];
                        IntegrationConfig c = integrationStore.get(id);
                        if (c != null && orgId.equals(c.getOrganizationId())) {
                            return Optional.of(c);
                        }
                        return Optional.empty();
                    }
                    if ("save".equals(name)) {
                        IntegrationConfig c = (IntegrationConfig) args[0];
                        if (c.getId() == null) c.setId(UUID.randomUUID());
                        integrationStore.put(c.getId(), c);
                        return c;
                    }
                    return null;
                }
        );

        adminSettingsService = new AdminSettingsService(settingsRepo, integrationRepo);
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Retrieve and update organization operational settings")
    public void testGetAndUpdateSettings() {
        OrganizationSettingsDto initial = adminSettingsService.getSettings();
        assertNotNull(initial);
        assertEquals(tenantId, initial.getOrganizationId());
        assertEquals(new BigDecimal("5000.00"), initial.getApprovalThresholdAmount());
        assertEquals(new BigDecimal("1.00"), initial.getMatchingToleranceAmount());

        Map<String, Object> updates = Map.of(
                "approvalThresholdAmount", "10000.00",
                "matchingToleranceAmount", "5.00",
                "autoExportApproved", "true"
        );
        OrganizationSettingsDto updated = adminSettingsService.updateSettings(updates);
        assertEquals(new BigDecimal("10000.00"), updated.getApprovalThresholdAmount());
        assertEquals(new BigDecimal("5.00"), updated.getMatchingToleranceAmount());
        assertTrue(updated.isAutoExportApproved());
    }

    @Test
    @DisplayName("Create integration with masked API key, update, and test connectivity")
    public void testIntegrationLifecycle() {
        CreateIntegrationRequest createReq = new CreateIntegrationRequest();
        createReq.setName("NetSuite ERP Production");
        createReq.setIntegrationType("ERP_NETSUITE");
        createReq.setEndpointUrl("https://api.netsuite.com/rest/v1");
        createReq.setApiKey("secret-token-abcdef12345");

        IntegrationConfigDto created = adminSettingsService.createIntegration(createReq);
        assertNotNull(created.getId());
        assertEquals("NetSuite ERP Production", created.getName());
        assertEquals("...2345", created.getApiKeyMasked());
        assertEquals("ACTIVE", created.getStatus());

        List<IntegrationConfigDto> list = adminSettingsService.getIntegrations();
        assertEquals(1, list.size());

        Map<String, Object> testResult = adminSettingsService.testIntegration(created.getId());
        assertEquals("CONNECTED", testResult.get("status"));
        assertEquals("SUCCESS", testResult.get("result"));
    }
}
