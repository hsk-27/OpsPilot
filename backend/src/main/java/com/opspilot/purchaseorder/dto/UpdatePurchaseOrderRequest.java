package com.opspilot.purchaseorder.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdatePurchaseOrderRequest {

    private String status;
    private BigDecimal totalAmount;
    private UUID supplierId;

    public UpdatePurchaseOrderRequest() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }
}
