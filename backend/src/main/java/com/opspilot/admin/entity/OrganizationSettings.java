package com.opspilot.admin.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_settings")
public class OrganizationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @Column(name = "approval_threshold_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal approvalThresholdAmount = new BigDecimal("5000.00");

    @Column(name = "matching_tolerance_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal matchingToleranceAmount = new BigDecimal("1.00");

    @Column(name = "duplicate_window_days", nullable = false)
    private int duplicateWindowDays = 90;

    @Column(name = "auto_export_approved", nullable = false)
    private boolean autoExportApproved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public OrganizationSettings() {}

    public OrganizationSettings(UUID organizationId) {
        this.organizationId = organizationId;
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

    public BigDecimal getApprovalThresholdAmount() {
        return approvalThresholdAmount;
    }

    public void setApprovalThresholdAmount(BigDecimal approvalThresholdAmount) {
        this.approvalThresholdAmount = approvalThresholdAmount;
    }

    public BigDecimal getMatchingToleranceAmount() {
        return matchingToleranceAmount;
    }

    public void setMatchingToleranceAmount(BigDecimal matchingToleranceAmount) {
        this.matchingToleranceAmount = matchingToleranceAmount;
    }

    public int getDuplicateWindowDays() {
        return duplicateWindowDays;
    }

    public void setDuplicateWindowDays(int duplicateWindowDays) {
        this.duplicateWindowDays = duplicateWindowDays;
    }

    public boolean isAutoExportApproved() {
        return autoExportApproved;
    }

    public void setAutoExportApproved(boolean autoExportApproved) {
        this.autoExportApproved = autoExportApproved;
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
