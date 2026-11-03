package com.opspilot.validation.dto;

import java.math.BigDecimal;

public class PoMatchResult {
    public enum Outcome {
        MATCHED,
        PARTIALLY_MATCHED,
        MISMATCHED,
        NO_PURCHASE_ORDER,
        NEEDS_REVIEW
    }

    private Outcome outcome;
    private String purchaseOrderNumber;
    private boolean supplierMatches;
    private boolean currencyMatches;
    private BigDecimal invoiceTotal;
    private BigDecimal poTotal;
    private BigDecimal amountDifference;
    private String message;

    public PoMatchResult() {}

    public static PoMatchResult noPurchaseOrder() {
        PoMatchResult r = new PoMatchResult();
        r.setOutcome(Outcome.NO_PURCHASE_ORDER);
        r.setMessage("No purchase order reference attached to invoice.");
        return r;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }

    public void setPurchaseOrderNumber(String purchaseOrderNumber) {
        this.purchaseOrderNumber = purchaseOrderNumber;
    }

    public boolean isSupplierMatches() {
        return supplierMatches;
    }

    public void setSupplierMatches(boolean supplierMatches) {
        this.supplierMatches = supplierMatches;
    }

    public boolean isCurrencyMatches() {
        return currencyMatches;
    }

    public void setCurrencyMatches(boolean currencyMatches) {
        this.currencyMatches = currencyMatches;
    }

    public BigDecimal getInvoiceTotal() {
        return invoiceTotal;
    }

    public void setInvoiceTotal(BigDecimal invoiceTotal) {
        this.invoiceTotal = invoiceTotal;
    }

    public BigDecimal getPoTotal() {
        return poTotal;
    }

    public void setPoTotal(BigDecimal poTotal) {
        this.poTotal = poTotal;
    }

    public BigDecimal getAmountDifference() {
        return amountDifference;
    }

    public void setAmountDifference(BigDecimal amountDifference) {
        this.amountDifference = amountDifference;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
