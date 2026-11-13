package com.opspilot.validation.service;

import com.opspilot.admin.entity.OrganizationSettings;
import com.opspilot.admin.repository.OrganizationSettingsRepository;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.entity.PurchaseOrderLine;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.validation.dto.LineMatchResult;
import com.opspilot.validation.dto.PoMatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PurchaseOrderMatchingService {

    private static final BigDecimal AMOUNT_TOLERANCE_PERCENT = new BigDecimal("0.02"); // 2% tolerance

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final OrganizationSettingsRepository settingsRepository;

    public PurchaseOrderMatchingService(PurchaseOrderRepository purchaseOrderRepository) {
        this(purchaseOrderRepository, null);
    }

    @Autowired
    public PurchaseOrderMatchingService(
            PurchaseOrderRepository purchaseOrderRepository,
            @Autowired(required = false) OrganizationSettingsRepository settingsRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.settingsRepository = settingsRepository;
    }

    public PoMatchResult matchInvoice(Invoice invoice) {
        if (invoice.getPurchaseOrderId() == null) {
            return PoMatchResult.noPurchaseOrder();
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findByIdAndOrganizationId(
                invoice.getPurchaseOrderId(),
                invoice.getOrganizationId()
        );

        if (poOpt.isEmpty()) {
            PoMatchResult res = new PoMatchResult();
            res.setOutcome(PoMatchResult.Outcome.MISMATCHED);
            res.setMessage("Referenced purchase order does not exist in this organization.");
            return res;
        }

        PurchaseOrder po = poOpt.get();
        PoMatchResult result = new PoMatchResult();
        result.setPurchaseOrderNumber(po.getPurchaseOrderNumber());
        result.setInvoiceTotal(invoice.getTotalAmount());
        result.setPoTotal(po.getTotalAmount());

        // 1. Supplier Match
        boolean supplierMatches = invoice.getSupplierId() != null && invoice.getSupplierId().equals(po.getSupplierId());
        result.setSupplierMatches(supplierMatches);

        // 2. Currency Match
        boolean currencyMatches = invoice.getCurrency() != null && invoice.getCurrency().equalsIgnoreCase(po.getCurrency());
        result.setCurrencyMatches(currencyMatches);

        // 3. Amount Comparison
        BigDecimal invTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal poTotal = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal diff = invTotal.subtract(poTotal);
        result.setAmountDifference(diff);

        if (!supplierMatches) {
            result.setOutcome(PoMatchResult.Outcome.MISMATCHED);
            result.setMessage("Supplier on invoice does not match purchase order supplier.");
            return result;
        }

        if (!currencyMatches) {
            result.setOutcome(PoMatchResult.Outcome.MISMATCHED);
            result.setMessage(String.format("Currency mismatch: invoice is in %s, purchase order is in %s.", invoice.getCurrency(), po.getCurrency()));
            return result;
        }

        // 4. Line-Level Matching Breakdown
        List<LineMatchResult> lineMatchResults = performLineMatching(invoice, po);
        result.setLineMatches(lineMatchResults);

        int matchedCount = 0;
        int invoiceLinesCount = invoice.getLines() != null ? invoice.getLines().size() : 0;
        for (LineMatchResult lmr : lineMatchResults) {
            if (lmr.isMatched() && !"UNBILLED_PO_LINE".equals(lmr.getMatchType())) {
                matchedCount++;
            }
        }
        result.setMatchedLinesCount(matchedCount);
        result.setTotalLinesCount(invoiceLinesCount);

        boolean hasLineDiscrepancy = invoiceLinesCount > 0 && matchedCount < invoiceLinesCount;

        // 5. Amount tolerance & outcome evaluation:
        if (diff.compareTo(BigDecimal.ZERO) <= 0) {
            if (hasLineDiscrepancy) {
                result.setOutcome(PoMatchResult.Outcome.PARTIALLY_MATCHED);
                result.setMessage(String.format("Invoice total is within PO authorized amount, but %d of %d line item(s) have variances.",
                        (invoiceLinesCount - matchedCount), invoiceLinesCount));
            } else {
                result.setOutcome(PoMatchResult.Outcome.MATCHED);
                result.setMessage("Invoice total and line items match purchase order authorized amounts.");
            }
        } else {
            BigDecimal maxAllowedOver = poTotal.multiply(AMOUNT_TOLERANCE_PERCENT).setScale(2, RoundingMode.HALF_UP);
            if (settingsRepository != null && invoice.getOrganizationId() != null) {
                try {
                    BigDecimal configuredTolerance = settingsRepository.findByOrganizationId(invoice.getOrganizationId())
                            .map(OrganizationSettings::getMatchingToleranceAmount)
                            .orElse(BigDecimal.ZERO);
                    if (configuredTolerance.compareTo(maxAllowedOver) > 0) {
                        maxAllowedOver = configuredTolerance;
                    }
                } catch (Exception ignored) {}
            }

            if (diff.compareTo(maxAllowedOver) <= 0) {
                result.setOutcome(PoMatchResult.Outcome.PARTIALLY_MATCHED);
                result.setMessage(String.format("Invoice exceeds PO total by %s, which is within the allowed tolerance threshold (%s).", diff, maxAllowedOver));
            } else {
                result.setOutcome(PoMatchResult.Outcome.NEEDS_REVIEW);
                result.setMessage(String.format("Invoice exceeds purchase order amount by %s, which exceeds tolerance threshold (%s).", diff, maxAllowedOver));
            }
        }

        return result;
    }

    private List<LineMatchResult> performLineMatching(Invoice invoice, PurchaseOrder po) {
        List<LineMatchResult> results = new ArrayList<>();
        List<InvoiceLine> invLines = invoice.getLines();
        List<PurchaseOrderLine> poLines = po.getLines();

        if (invLines == null || invLines.isEmpty() || poLines == null || poLines.isEmpty()) {
            return results;
        }

        Set<Integer> matchedPoIndices = new HashSet<>();

        for (int i = 0; i < invLines.size(); i++) {
            InvoiceLine invLine = invLines.get(i);
            String invDesc = invLine.getDescription() != null ? invLine.getDescription().trim() : "";
            BigDecimal invQty = invLine.getQuantity() != null ? invLine.getQuantity() : BigDecimal.ONE;
            BigDecimal invPrice = invLine.getUnitPrice() != null ? invLine.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal invLineTotal = invLine.getLineTotal() != null ? invLine.getLineTotal() : invQty.multiply(invPrice);

            // Find best matching PO line
            int matchedPoIdx = -1;

            // 1. Exact match on description
            for (int j = 0; j < poLines.size(); j++) {
                if (!matchedPoIndices.contains(j)) {
                    PurchaseOrderLine poLine = poLines.get(j);
                    if (poLine.getDescription() != null && poLine.getDescription().trim().equalsIgnoreCase(invDesc)) {
                        matchedPoIdx = j;
                        break;
                    }
                }
            }

            // 2. Substring match on description
            if (matchedPoIdx == -1 && !invDesc.isBlank()) {
                for (int j = 0; j < poLines.size(); j++) {
                    if (!matchedPoIndices.contains(j)) {
                        PurchaseOrderLine poLine = poLines.get(j);
                        String poDesc = poLine.getDescription() != null ? poLine.getDescription().trim() : "";
                        if (!poDesc.isBlank() && (poDesc.toLowerCase().contains(invDesc.toLowerCase()) || invDesc.toLowerCase().contains(poDesc.toLowerCase()))) {
                            matchedPoIdx = j;
                            break;
                        }
                    }
                }
            }

            if (matchedPoIdx != -1) {
                matchedPoIndices.add(matchedPoIdx);
                PurchaseOrderLine poLine = poLines.get(matchedPoIdx);
                BigDecimal poQty = poLine.getQuantity() != null ? poLine.getQuantity() : BigDecimal.ONE;
                BigDecimal poPrice = poLine.getUnitPrice() != null ? poLine.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal poLineTotal = poLine.getLineTotal() != null ? poLine.getLineTotal() : poQty.multiply(poPrice);
                BigDecimal lineVariance = invLineTotal.subtract(poLineTotal);

                boolean qtyWithin = invQty.compareTo(poQty) <= 0;
                boolean priceWithin = invPrice.compareTo(poPrice) <= 0;

                boolean matched = qtyWithin && priceWithin;
                String matchType;
                String note;

                if (invQty.compareTo(poQty) == 0 && invPrice.compareTo(poPrice) == 0) {
                    matchType = "EXACT";
                    note = "Exact match on quantity and unit price";
                } else if (!qtyWithin && !priceWithin) {
                    matchType = "QTY_AND_PRICE_VARIANCE";
                    note = String.format("Quantity (%s > %s) and unit price (%s > %s) exceed PO limit", invQty, poQty, invPrice, poPrice);
                } else if (!qtyWithin) {
                    matchType = "QUANTITY_VARIANCE";
                    note = String.format("Billed quantity %s exceeds PO quantity %s", invQty, poQty);
                } else if (!priceWithin) {
                    matchType = "PRICE_VARIANCE";
                    note = String.format("Unit price %s exceeds authorized PO price %s", invPrice, poPrice);
                } else {
                    matchType = "PARTIAL_BILLING";
                    note = String.format("Partial quantity (%s of %s) billed within PO limit", invQty, poQty);
                }

                results.add(new LineMatchResult(
                        invDesc,
                        poLine.getDescription(),
                        invQty,
                        poQty,
                        invPrice,
                        poPrice,
                        invLineTotal,
                        poLineTotal,
                        lineVariance,
                        matched,
                        matchType,
                        note
                ));
            } else {
                results.add(new LineMatchResult(
                        invDesc,
                        null,
                        invQty,
                        BigDecimal.ZERO,
                        invPrice,
                        BigDecimal.ZERO,
                        invLineTotal,
                        BigDecimal.ZERO,
                        invLineTotal,
                        false,
                        "UNMATCHED",
                        "Line item not found on referenced purchase order"
                ));
            }
        }

        // Add remaining unbilled PO lines for operational visibility
        for (int j = 0; j < poLines.size(); j++) {
            if (!matchedPoIndices.contains(j)) {
                PurchaseOrderLine poLine = poLines.get(j);
                BigDecimal poQty = poLine.getQuantity() != null ? poLine.getQuantity() : BigDecimal.ONE;
                BigDecimal poPrice = poLine.getUnitPrice() != null ? poLine.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal poLineTotal = poLine.getLineTotal() != null ? poLine.getLineTotal() : poQty.multiply(poPrice);

                results.add(new LineMatchResult(
                        "(Unbilled PO Line)",
                        poLine.getDescription(),
                        BigDecimal.ZERO,
                        poQty,
                        BigDecimal.ZERO,
                        poPrice,
                        BigDecimal.ZERO,
                        poLineTotal,
                        poLineTotal.negate(),
                        true,
                        "UNBILLED_PO_LINE",
                        "Authorized PO line item remaining open"
                ));
            }
        }

        return results;
    }
}

