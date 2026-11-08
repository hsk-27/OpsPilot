package com.opspilot.analytics;

import com.opspilot.ai.repository.AiInvocationRepository;
import com.opspilot.analytics.dto.OverviewAnalyticsDto;
import com.opspilot.analytics.service.AnalyticsService;
import com.opspilot.approval.entity.ApprovalTask;
import com.opspilot.approval.repository.ApprovalTaskRepository;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.export.repository.ExportRecordRepository;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.entity.WorkflowExecution;
import com.opspilot.workflow.repository.WorkflowExecutionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AnalyticsServiceTest {

    private AnalyticsService analyticsService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        TenantContext.setCurrentTenant(tenantId);

        InvoiceRepository invoiceRepo = (InvoiceRepository) Proxy.newProxyInstance(
                InvoiceRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("countByOrganizationId".equals(name)) return 10L;
                    if ("countByOrganizationIdAndProcessingStatus".equals(name)) {
                        ProcessingStatus s = (ProcessingStatus) args[1];
                        if (s == ProcessingStatus.APPROVED) return 6L;
                        if (s == ProcessingStatus.NEEDS_REVIEW) return 2L;
                        if (s == ProcessingStatus.REJECTED) return 1L;
                        if (s == ProcessingStatus.EXPORTED) return 5L;
                        return 0L;
                    }
                    if ("countByOrganizationIdAndApprovalStatus".equals(name)) {
                        ApprovalStatus a = (ApprovalStatus) args[1];
                        if (a == ApprovalStatus.APPROVED) return 6L;
                        if (a == ApprovalStatus.REJECTED) return 1L;
                        if (a == ApprovalStatus.CHANGES_REQUESTED) return 1L;
                        return 0L;
                    }
                    return null;
                }
        );

        ApprovalTaskRepository approvalRepo = (ApprovalTaskRepository) Proxy.newProxyInstance(
                ApprovalTaskRepository.class.getClassLoader(),
                new Class<?>[]{ApprovalTaskRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("countByOrganizationIdAndStatus".equals(name)) {
                        ApprovalTask.Status s = (ApprovalTask.Status) args[1];
                        if (s == ApprovalTask.Status.PENDING) return 3L;
                        if (s == ApprovalTask.Status.DECIDED) return 7L;
                        if (s == ApprovalTask.Status.CANCELLED) return 0L;
                    }
                    return 0L;
                }
        );

        WorkflowExecutionRepository execRepo = (WorkflowExecutionRepository) Proxy.newProxyInstance(
                WorkflowExecutionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowExecutionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("countByOrganizationId".equals(name)) return 20L;
                    if ("countByOrganizationIdAndStatus".equals(name)) {
                        WorkflowExecution.Status s = (WorkflowExecution.Status) args[1];
                        if (s == WorkflowExecution.Status.COMPLETED) return 18L;
                        if (s == WorkflowExecution.Status.FAILED) return 2L;
                        if (s == WorkflowExecution.Status.RUNNING) return 0L;
                        if (s == WorkflowExecution.Status.WAITING_APPROVAL) return 0L;
                        if (s == WorkflowExecution.Status.CANCELLED) return 0L;
                    }
                    return 0L;
                }
        );

        AiInvocationRepository aiRepo = (AiInvocationRepository) Proxy.newProxyInstance(
                AiInvocationRepository.class.getClassLoader(),
                new Class<?>[]{AiInvocationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("countByOrganizationId".equals(name)) return 15L;
                    if ("getTotalInputTokens".equals(name)) return 12000L;
                    if ("getTotalOutputTokens".equals(name)) return 4000L;
                    if ("getTotalEstimatedCost".equals(name)) return new BigDecimal("0.024");
                    if ("getAverageLatency".equals(name)) return 85.0;
                    return null;
                }
        );

        ExportRecordRepository exportRepo = (ExportRecordRepository) Proxy.newProxyInstance(
                ExportRecordRepository.class.getClassLoader(),
                new Class<?>[]{ExportRecordRepository.class},
                (proxy, method, args) -> null
        );

        analyticsService = new AnalyticsService(invoiceRepo, approvalRepo, execRepo, aiRepo, exportRepo);
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Calculate overview operational KPIs and success rates")
    public void testOverviewMetrics() {
        OverviewAnalyticsDto overview = analyticsService.getOverview();
        assertNotNull(overview);
        assertEquals(10L, overview.getTotalInvoices());
        assertEquals(6L, overview.getApprovedInvoices());
        assertEquals(2L, overview.getInvoicesPendingReview());
        assertEquals(3L, overview.getPendingApprovals());
        assertEquals(90.0, overview.getWorkflowSuccessRate()); // 18 / 20 = 90%
        assertEquals(16000L, overview.getTotalAiTokens());
        assertEquals(85.0, overview.getAvgAiLatencyMs());
    }

    @Test
    @DisplayName("Get workflow execution breakdown")
    public void testWorkflowAnalytics() {
        Map<String, Object> map = analyticsService.getWorkflowAnalytics();
        assertEquals(20L, map.get("totalExecutions"));
        assertEquals(18L, map.get("completedExecutions"));
        assertEquals(2L, map.get("failedExecutions"));
        assertEquals(90.0, map.get("successRatePercent"));
    }

    @Test
    @DisplayName("Get AI usage analytics and telemetry")
    public void testAiUsageAnalytics() {
        Map<String, Object> map = analyticsService.getAiUsageAnalytics();
        assertEquals(15L, map.get("totalInvocations"));
        assertEquals(16000L, map.get("totalTokens"));
        assertEquals(new BigDecimal("0.024"), map.get("estimatedCostUsd"));
    }

    @Test
    @DisplayName("Get approval task metrics and ratios")
    public void testApprovalAnalytics() {
        Map<String, Object> map = analyticsService.getApprovalAnalytics();
        assertEquals(3L, map.get("pendingTasks"));
        assertEquals(7L, map.get("decidedTasks"));
        assertEquals(6L, map.get("approvedInvoices"));
        assertEquals(1L, map.get("rejectedInvoices"));
    }
}
