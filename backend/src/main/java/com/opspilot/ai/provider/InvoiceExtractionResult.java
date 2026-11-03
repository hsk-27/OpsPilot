package com.opspilot.ai.provider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceExtractionResult {

    private String supplierName;
    private String supplierTaxIdentifier;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currency = "USD";
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private List<ExtractedLineItem> lineItems = new ArrayList<>();
    private Map<String, Double> fieldConfidence = new HashMap<>();
    private Map<String, String> evidence = new HashMap<>();
    private long latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal estimatedCost;
    private boolean success = true;
    private String errorMessage;

    public InvoiceExtractionResult() {}

    public static InvoiceExtractionResult failure(String errorMessage, long latencyMs) {
        InvoiceExtractionResult res = new InvoiceExtractionResult();
        res.setSuccess(false);
        res.setErrorMessage(errorMessage);
        res.setLatencyMs(latencyMs);
        return res;
    }

    public double getAverageConfidence() {
        if (fieldConfidence == null || fieldConfidence.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (double val : fieldConfidence.values()) {
            sum += val;
        }
        return sum / fieldConfidence.size();
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierTaxIdentifier() {
        return supplierTaxIdentifier;
    }

    public void setSupplierTaxIdentifier(String supplierTaxIdentifier) {
        this.supplierTaxIdentifier = supplierTaxIdentifier;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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

    public List<ExtractedLineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<ExtractedLineItem> lineItems) {
        this.lineItems = lineItems;
    }

    public Map<String, Double> getFieldConfidence() {
        return fieldConfidence;
    }

    public void setFieldConfidence(Map<String, Double> fieldConfidence) {
        this.fieldConfidence = fieldConfidence;
    }

    public Map<String, String> getEvidence() {
        return evidence;
    }

    public void setEvidence(Map<String, String> evidence) {
        this.evidence = evidence;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static class ExtractedLineItem {
        private String description;
        private BigDecimal quantity = BigDecimal.ONE;
        private BigDecimal unitPrice = BigDecimal.ZERO;
        private BigDecimal taxRate = BigDecimal.ZERO;
        private BigDecimal lineTotal = BigDecimal.ZERO;

        public ExtractedLineItem() {}

        public ExtractedLineItem(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate, BigDecimal lineTotal) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.taxRate = taxRate;
            this.lineTotal = lineTotal;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getTaxRate() {
            return taxRate;
        }

        public void setTaxRate(BigDecimal taxRate) {
            this.taxRate = taxRate;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }

        public void setLineTotal(BigDecimal lineTotal) {
            this.lineTotal = lineTotal;
        }
    }
}
