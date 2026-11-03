package com.opspilot.approval.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_tasks")
public class ApprovalTask {

    public enum Status {
        PENDING,
        DECIDED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "workflow_execution_id")
    private UUID workflowExecutionId;

    @Column(name = "node_execution_id")
    private UUID nodeExecutionId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "assigned_role")
    private String assignedRole = "APPROVER";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "decision_deadline")
    private Instant decisionDeadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ApprovalTask() {}

    public ApprovalTask(UUID organizationId, UUID workflowExecutionId, UUID nodeExecutionId, UUID invoiceId, UUID assignedUserId, String assignedRole) {
        this.organizationId = organizationId;
        this.workflowExecutionId = workflowExecutionId;
        this.nodeExecutionId = nodeExecutionId;
        this.invoiceId = invoiceId;
        this.assignedUserId = assignedUserId;
        this.assignedRole = assignedRole != null ? assignedRole : "APPROVER";
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
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

    public UUID getNodeExecutionId() {
        return nodeExecutionId;
    }

    public void setNodeExecutionId(UUID nodeExecutionId) {
        this.nodeExecutionId = nodeExecutionId;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
