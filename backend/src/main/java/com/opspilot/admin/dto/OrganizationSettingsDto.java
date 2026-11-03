package com.opspilot.admin.dto;

import com.opspilot.admin.entity.OrganizationSettings;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class OrganizationSettingsDto {

    private UUID id;
    private UUID organizationId;
    private BigDecimal approvalThresholdAmount;
    private BigDecimal matchingToleranceAmount;
    private int duplicateWindowDays;
    private boolean autoExportApproved;
    private Instant updatedAt;

    public OrganizationSettingsDto() {}

    public static OrganizationSettingsDto from(OrganizationSettings entity) {
        if (entity == null) return null;
        OrganizationSettingsDto dto = new OrganizationSettingsDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setApprovalThresholdAmount(entity.getApprovalThresholdAmount());
        dto.setMatchingToleranceAmount(entity.getMatchingToleranceAmount());
        dto.setDuplicateWindowDays(entity.getDuplicateWindowDays());
        dto.setAutoExportApproved(entity.isAutoExportApproved());
        dto.setUpdatedAt(entity.getUpdatedAt());
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
