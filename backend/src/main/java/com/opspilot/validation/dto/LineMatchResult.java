package com.opspilot.validation.dto;

import java.math.BigDecimal;

public class LineMatchResult {
    private String invoiceLineDescription;
    private String poLineDescription;
    private BigDecimal invoiceQuantity;
    private BigDecimal poQuantity;
    private BigDecimal invoiceUnitPrice;
    private BigDecimal poUnitPrice;
    private BigDecimal invoiceLineTotal;
    private BigDecimal poLineTotal;
    private BigDecimal varianceAmount;
    private boolean matched;
    private String matchType;
    private String discrepancyNote;

    public LineMatchResult() {}

    public LineMatchResult(
            String invoiceLineDescription,
            String poLineDescription,
            BigDecimal invoiceQuantity,
            BigDecimal poQuantity,
            BigDecimal invoiceUnitPrice,
            BigDecimal poUnitPrice,
            BigDecimal invoiceLineTotal,
            BigDecimal poLineTotal,
            BigDecimal varianceAmount,
            boolean matched,
            String matchType,
            String discrepancyNote) {
        this.invoiceLineDescription = invoiceLineDescription;
        this.poLineDescription = poLineDescription;
        this.invoiceQuantity = invoiceQuantity;
        this.poQuantity = poQuantity;
        this.invoiceUnitPrice = invoiceUnitPrice;
        this.poUnitPrice = poUnitPrice;
        this.invoiceLineTotal = invoiceLineTotal;
        this.poLineTotal = poLineTotal;
        this.varianceAmount = varianceAmount;
        this.matched = matched;
        this.matchType = matchType;
        this.discrepancyNote = discrepancyNote;
    }

    public String getInvoiceLineDescription() {
        return invoiceLineDescription;
    }

    public void setInvoiceLineDescription(String invoiceLineDescription) {
        this.invoiceLineDescription = invoiceLineDescription;
    }

    public String getPoLineDescription() {
        return poLineDescription;
    }

    public void setPoLineDescription(String poLineDescription) {
        this.poLineDescription = poLineDescription;
    }

    public BigDecimal getInvoiceQuantity() {
        return invoiceQuantity;
    }

    public void setInvoiceQuantity(BigDecimal invoiceQuantity) {
        this.invoiceQuantity = invoiceQuantity;
    }

    public BigDecimal getPoQuantity() {
        return poQuantity;
    }

    public void setPoQuantity(BigDecimal poQuantity) {
        this.poQuantity = poQuantity;
    }

    public BigDecimal getInvoiceUnitPrice() {
        return invoiceUnitPrice;
    }

    public void setInvoiceUnitPrice(BigDecimal invoiceUnitPrice) {
        this.invoiceUnitPrice = invoiceUnitPrice;
    }

    public BigDecimal getPoUnitPrice() {
        return poUnitPrice;
    }

    public void setPoUnitPrice(BigDecimal poUnitPrice) {
        this.poUnitPrice = poUnitPrice;
    }

    public BigDecimal getInvoiceLineTotal() {
        return invoiceLineTotal;
    }

    public void setInvoiceLineTotal(BigDecimal invoiceLineTotal) {
        this.invoiceLineTotal = invoiceLineTotal;
    }

    public BigDecimal getPoLineTotal() {
        return poLineTotal;
    }

    public void setPoLineTotal(BigDecimal poLineTotal) {
        this.poLineTotal = poLineTotal;
    }

    public BigDecimal getVarianceAmount() {
        return varianceAmount;
    }

    public void setVarianceAmount(BigDecimal varianceAmount) {
        this.varianceAmount = varianceAmount;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getDiscrepancyNote() {
        return discrepancyNote;
    }

    public void setDiscrepancyNote(String discrepancyNote) {
        this.discrepancyNote = discrepancyNote;
    }
}
