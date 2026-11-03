package com.opspilot.approval.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_decisions")
public class ApprovalDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "approval_task_id", nullable = false)
    private UUID approvalTaskId;

    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionType decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "previous_data", columnDefinition = "TEXT")
    private String previousData;

    @Column(name = "modified_data", columnDefinition = "TEXT")
    private String modifiedData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ApprovalDecision() {}

    public ApprovalDecision(UUID approvalTaskId, UUID decidedBy, DecisionType decision, String comments, String previousData, String modifiedData) {
        this.approvalTaskId = approvalTaskId;
        this.decidedBy = decidedBy;
        this.decision = decision;
        this.comments = comments;
        this.previousData = previousData;
        this.modifiedData = modifiedData;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getApprovalTaskId() {
        return approvalTaskId;
    }

    public void setApprovalTaskId(UUID approvalTaskId) {
        this.approvalTaskId = approvalTaskId;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public DecisionType getDecision() {
        return decision;
    }

    public void setDecision(DecisionType decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPreviousData() {
        return previousData;
    }

    public void setPreviousData(String previousData) {
        this.previousData = previousData;
    }

    public String getModifiedData() {
        return modifiedData;
    }

    public void setModifiedData(String modifiedData) {
        this.modifiedData = modifiedData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
