package com.opspilot.approval.dto;

import com.opspilot.approval.entity.ApprovalTask;
import com.opspilot.invoice.dto.InvoiceDto;

import java.time.Instant;
import java.util.UUID;

public class ApprovalTaskDto {
    private UUID id;
    private UUID organizationId;
    private UUID workflowExecutionId;
    private UUID invoiceId;
    private UUID assignedUserId;
    private String assignedRole;
    private ApprovalTask.Status status;
    private Instant decisionDeadline;
    private Instant createdAt;
    private InvoiceDto invoice;

    public ApprovalTaskDto() {}

    public static ApprovalTaskDto from(ApprovalTask task, InvoiceDto invoice) {
        ApprovalTaskDto dto = new ApprovalTaskDto();
        dto.setId(task.getId());
        dto.setOrganizationId(task.getOrganizationId());
        dto.setWorkflowExecutionId(task.getWorkflowExecutionId());
        dto.setInvoiceId(task.getInvoiceId());
        dto.setAssignedUserId(task.getAssignedUserId());
        dto.setAssignedRole(task.getAssignedRole());
        dto.setStatus(task.getStatus());
        dto.setDecisionDeadline(task.getDecisionDeadline());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setInvoice(invoice);
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

    public UUID getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(UUID workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public UUID getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(UUID assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public String getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(String assignedRole) {
        this.assignedRole = assignedRole;
    }

    public ApprovalTask.Status getStatus() {
        return status;
    }

    public void setStatus(ApprovalTask.Status status) {
        this.status = status;
    }

    public Instant getDecisionDeadline() {
        return decisionDeadline;
    }

    public void setDecisionDeadline(Instant decisionDeadline) {
        this.decisionDeadline = decisionDeadline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public InvoiceDto getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceDto invoice) {
        this.invoice = invoice;
    }
}
