package com.opspilot.analytics.dto;

import java.math.BigDecimal;

public class OverviewAnalyticsDto {
    private long totalInvoices;
    private long invoicesPendingReview;
    private long pendingApprovals;
    private long approvedInvoices;
    private long rejectedInvoices;
    private long exportedInvoices;
    private double workflowSuccessRate;
    private double avgProcessingTimeSec;
    private long failedExecutions;
    private long totalAiTokens;
    private BigDecimal estimatedAiCost;
    private double avgAiLatencyMs;

    public OverviewAnalyticsDto() {
        this.estimatedAiCost = BigDecimal.ZERO;
    }

    public long getTotalInvoices() {
        return totalInvoices;
    }

    public void setTotalInvoices(long totalInvoices) {
        this.totalInvoices = totalInvoices;
    }

    public long getInvoicesPendingReview() {
        return invoicesPendingReview;
    }

    public void setInvoicesPendingReview(long invoicesPendingReview) {
        this.invoicesPendingReview = invoicesPendingReview;
    }

    public long getPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(long pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
    }

    public long getApprovedInvoices() {
        return approvedInvoices;
    }

    public void setApprovedInvoices(long approvedInvoices) {
        this.approvedInvoices = approvedInvoices;
    }

    public long getRejectedInvoices() {
        return rejectedInvoices;
    }

    public void setRejectedInvoices(long rejectedInvoices) {
        this.rejectedInvoices = rejectedInvoices;
    }

    public long getExportedInvoices() {
        return exportedInvoices;
    }

    public void setExportedInvoices(long exportedInvoices) {
        this.exportedInvoices = exportedInvoices;
    }

    public double getWorkflowSuccessRate() {
        return workflowSuccessRate;
    }

    public void setWorkflowSuccessRate(double workflowSuccessRate) {
        this.workflowSuccessRate = workflowSuccessRate;
    }

    public double getAvgProcessingTimeSec() {
        return avgProcessingTimeSec;
    }

    public void setAvgProcessingTimeSec(double avgProcessingTimeSec) {
        this.avgProcessingTimeSec = avgProcessingTimeSec;
    }

    public long getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(long failedExecutions) {
        this.failedExecutions = failedExecutions;
    }

    public long getTotalAiTokens() {
        return totalAiTokens;
    }

    public void setTotalAiTokens(long totalAiTokens) {
        this.totalAiTokens = totalAiTokens;
    }

    public BigDecimal getEstimatedAiCost() {
        return estimatedAiCost;
    }

    public void setEstimatedAiCost(BigDecimal estimatedAiCost) {
        this.estimatedAiCost = estimatedAiCost;
    }

    public double getAvgAiLatencyMs() {
        return avgAiLatencyMs;
    }

    public void setAvgAiLatencyMs(double avgAiLatencyMs) {
        this.avgAiLatencyMs = avgAiLatencyMs;
    }
}
