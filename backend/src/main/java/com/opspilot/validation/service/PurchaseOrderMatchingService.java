package com.opspilot.validation.service;

import com.opspilot.admin.entity.OrganizationSettings;
import com.opspilot.admin.repository.OrganizationSettingsRepository;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.validation.dto.PoMatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

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

        // Amount tolerance check:
        // If invoice total <= PO total, or diff <= 2% of PO total or diff <= matchingToleranceAmount
        if (diff.compareTo(BigDecimal.ZERO) <= 0) {
            result.setOutcome(PoMatchResult.Outcome.MATCHED);
            result.setMessage("Invoice total is within purchase order authorized amount.");
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
}
