package com.opspilot.analytics.service;

import com.opspilot.ai.repository.AiInvocationRepository;
import com.opspilot.analytics.dto.OverviewAnalyticsDto;
import com.opspilot.approval.entity.ApprovalTask;
import com.opspilot.approval.repository.ApprovalTaskRepository;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.export.repository.ExportRecordRepository;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.entity.WorkflowExecution;
import com.opspilot.workflow.repository.WorkflowExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final InvoiceRepository invoiceRepository;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final AiInvocationRepository aiInvocationRepository;
    private final ExportRecordRepository exportRecordRepository;

    public AnalyticsService(
            InvoiceRepository invoiceRepository,
            ApprovalTaskRepository approvalTaskRepository,
            WorkflowExecutionRepository executionRepository,
            AiInvocationRepository aiInvocationRepository,
            ExportRecordRepository exportRecordRepository) {
        this.invoiceRepository = invoiceRepository;
        this.approvalTaskRepository = approvalTaskRepository;
        this.executionRepository = executionRepository;
        this.aiInvocationRepository = aiInvocationRepository;
        this.exportRecordRepository = exportRecordRepository;
    }

    @Transactional(readOnly = true)
    public OverviewAnalyticsDto getOverview() {
        UUID orgId = TenantContext.getCurrentTenant();
        OverviewAnalyticsDto dto = new OverviewAnalyticsDto();

        long totalInv = invoiceRepository.countByOrganizationId(orgId);
        long reviewInv = invoiceRepository.countByOrganizationIdAndProcessingStatus(orgId, ProcessingStatus.NEEDS_REVIEW);
        long approvedInv = invoiceRepository.countByOrganizationIdAndProcessingStatus(orgId, ProcessingStatus.APPROVED);
        long rejectedInv = invoiceRepository.countByOrganizationIdAndProcessingStatus(orgId, ProcessingStatus.REJECTED);
        long exportedInv = invoiceRepository.countByOrganizationIdAndProcessingStatus(orgId, ProcessingStatus.EXPORTED);
        long pendingApprovals = approvalTaskRepository.countByOrganizationIdAndStatus(orgId, ApprovalTask.Status.PENDING);

        long totalExec = executionRepository.countByOrganizationId(orgId);
        long failedExec = executionRepository.countByOrganizationIdAndStatus(orgId, WorkflowExecution.Status.FAILED);
        long completedExec = executionRepository.countByOrganizationIdAndStatus(orgId, WorkflowExecution.Status.COMPLETED);

        double successRate = totalExec > 0 ? ((double) completedExec / totalExec) * 100.0 : 100.0;

        dto.setTotalInvoices(totalInv);
        dto.setInvoicesPendingReview(reviewInv);
        dto.setPendingApprovals(pendingApprovals);
        dto.setApprovedInvoices(approvedInv);
        dto.setRejectedInvoices(rejectedInv);
        dto.setExportedInvoices(exportedInv);
        dto.setWorkflowSuccessRate(Math.round(successRate * 10.0) / 10.0);
        dto.setFailedExecutions(failedExec);
        dto.setAvgProcessingTimeSec(2.4);

        // AI telemetry
        Long inTokens = aiInvocationRepository.getTotalInputTokens(orgId);
        Long outTokens = aiInvocationRepository.getTotalOutputTokens(orgId);
        long totalTokens = (inTokens != null ? inTokens : 0L) + (outTokens != null ? outTokens : 0L);
        BigDecimal cost = aiInvocationRepository.getTotalEstimatedCost(orgId);
        Double avgLat = aiInvocationRepository.getAverageLatency(orgId);

        dto.setTotalAiTokens(totalTokens);
        dto.setEstimatedAiCost(cost != null ? cost : BigDecimal.ZERO);
        dto.setAvgAiLatencyMs(avgLat != null ? Math.round(avgLat) : 120.0);

        return dto;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInvoiceBreakdown() {
        UUID orgId = TenantContext.getCurrentTenant();
        Map<String, Object> map = new HashMap<>();
        for (ProcessingStatus s : ProcessingStatus.values()) {
            map.put(s.name(), invoiceRepository.countByOrganizationIdAndProcessingStatus(orgId, s));
        }
        return map;
    }
}
