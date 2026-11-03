package com.opspilot.purchaseorder.dto;

import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.entity.PurchaseOrderLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PurchaseOrderDto {
    private UUID id;
    private UUID organizationId;
    private UUID supplierId;
    private String purchaseOrderNumber;
    private LocalDate issueDate;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String status;
    private Instant createdAt;
    private List<PurchaseOrderLineDto> lines;

    public PurchaseOrderDto() {}

    public static PurchaseOrderDto from(PurchaseOrder po) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(po.getId());
        dto.setOrganizationId(po.getOrganizationId());
        dto.setSupplierId(po.getSupplierId());
        dto.setPurchaseOrderNumber(po.getPurchaseOrderNumber());
        dto.setIssueDate(po.getIssueDate());
        dto.setCurrency(po.getCurrency());
        dto.setSubtotal(po.getSubtotal());
        dto.setTaxAmount(po.getTaxAmount());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setStatus(po.getStatus());
        dto.setCreatedAt(po.getCreatedAt());
        if (po.getLines() != null) {
            dto.setLines(po.getLines().stream().map(PurchaseOrderLineDto::from).collect(Collectors.toList()));
        }
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

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }

    public void setPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<PurchaseOrderLineDto> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseOrderLineDto> lines) {
        this.lines = lines;
    }
}
