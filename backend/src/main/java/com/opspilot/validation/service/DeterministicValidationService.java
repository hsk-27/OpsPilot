package com.opspilot.validation.service;

import com.opspilot.admin.entity.OrganizationSettings;
import com.opspilot.admin.repository.OrganizationSettingsRepository;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.validation.dto.RuleEvaluation;
import com.opspilot.validation.dto.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

@Service
public class DeterministicValidationService {

    private static final BigDecimal ROUNDING_TOLERANCE = new BigDecimal("0.05");

    private final InvoiceRepository invoiceRepository;
    private final SupplierRepository supplierRepository;
    private final OrganizationSettingsRepository settingsRepository;

    public DeterministicValidationService(InvoiceRepository invoiceRepository, SupplierRepository supplierRepository) {
        this(invoiceRepository, supplierRepository, null);
    }

    @Autowired
    public DeterministicValidationService(
            InvoiceRepository invoiceRepository,
            SupplierRepository supplierRepository,
            @Autowired(required = false) OrganizationSettingsRepository settingsRepository) {
        this.invoiceRepository = invoiceRepository;
        this.supplierRepository = supplierRepository;
        this.settingsRepository = settingsRepository;
    }

    public ValidationResult validateInvoice(Invoice invoice) {
        ValidationResult result = new ValidationResult();

        // 1. Structure Checks
        // 1.1 Invoice Number present
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().trim().isBlank()) {
            result.addEvaluation(RuleEvaluation.fail("RULE_INVOICE_NUMBER_PRESENT", "Invoice number is required", "invoiceNumber"));
        } else {
            result.addEvaluation(RuleEvaluation.pass("RULE_INVOICE_NUMBER_PRESENT", "Invoice number is present: " + invoice.getInvoiceNumber()));
        }

        // 1.2 Invoice Date present
        if (invoice.getInvoiceDate() == null) {
            result.addEvaluation(RuleEvaluation.fail("RULE_INVOICE_DATE_PRESENT", "Invoice date is missing or invalid", "invoiceDate"));
        } else {
            result.addEvaluation(RuleEvaluation.pass("RULE_INVOICE_DATE_PRESENT", "Invoice date is valid: " + invoice.getInvoiceDate()));
        }

        // 1.3 Currency validity (ISO 4217)
        if (invoice.getCurrency() == null || invoice.getCurrency().trim().length() != 3) {
            result.addEvaluation(RuleEvaluation.fail("RULE_CURRENCY_VALID", "Currency must be a 3-letter ISO code", "currency"));
        } else {
            try {
                Currency.getInstance(invoice.getCurrency().trim().toUpperCase());
                result.addEvaluation(RuleEvaluation.pass("RULE_CURRENCY_VALID", "Currency is valid ISO code: " + invoice.getCurrency()));
            } catch (IllegalArgumentException e) {
                result.addEvaluation(RuleEvaluation.fail("RULE_CURRENCY_VALID", "Unknown currency code: " + invoice.getCurrency(), "currency"));
            }
        }

        // 1.4 Supplier association
        if (invoice.getSupplierId() == null) {
            result.addEvaluation(RuleEvaluation.warning("RULE_SUPPLIER_RESOLVED", "Supplier is not linked to this invoice", "supplierId"));
        } else {
            boolean supplierActive = supplierRepository.findByIdAndOrganizationId(invoice.getSupplierId(), invoice.getOrganizationId())
                    .map(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                    .orElse(false);

            if (supplierActive) {
                result.addEvaluation(RuleEvaluation.pass("RULE_SUPPLIER_RESOLVED", "Supplier is resolved and active"));
            } else {
                result.addEvaluation(RuleEvaluation.fail("RULE_SUPPLIER_RESOLVED", "Supplier is inactive or not found", "supplierId"));
            }
        }

        // 2. Financial Math Checks
        BigDecimal subtotal = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        BigDecimal taxAmount = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;

        // 2.1 Non-negative checks
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            result.addEvaluation(RuleEvaluation.fail("RULE_NON_NEGATIVE_SUBTOTAL", "Subtotal cannot be negative", "subtotal"));
        }
        if (taxAmount.compareTo(BigDecimal.ZERO) < 0) {
            result.addEvaluation(RuleEvaluation.fail("RULE_NON_NEGATIVE_TAX", "Tax amount cannot be negative", "taxAmount"));
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            result.addEvaluation(RuleEvaluation.fail("RULE_POSITIVE_TOTAL", "Total amount must be greater than zero", "totalAmount"));
        } else {
            result.addEvaluation(RuleEvaluation.pass("RULE_POSITIVE_TOTAL", "Total amount is positive: " + totalAmount));
        }

        // 2.2 Mathematical consistency: subtotal + taxAmount = totalAmount within tolerance
        BigDecimal expectedTotal = subtotal.add(taxAmount);
        BigDecimal difference = expectedTotal.subtract(totalAmount).abs();
        if (difference.compareTo(ROUNDING_TOLERANCE) > 0) {
            result.addEvaluation(RuleEvaluation.fail(
                    "RULE_TOTAL_MATH_CONSISTENCY",
                    String.format("Calculated total (%s) differs from stated total (%s) by %s (tolerance %s)",
                            expectedTotal, totalAmount, difference, ROUNDING_TOLERANCE),
                    "totalAmount"
            ));
        } else {
            result.addEvaluation(RuleEvaluation.pass("RULE_TOTAL_MATH_CONSISTENCY", "Subtotal plus tax matches total amount"));
        }

        // 2.3 Line items sum check
        if (invoice.getLines() != null && !invoice.getLines().isEmpty()) {
            BigDecimal linesSum = BigDecimal.ZERO;
            for (InvoiceLine line : invoice.getLines()) {
                if (line.getLineTotal() != null) {
                    linesSum = linesSum.add(line.getLineTotal());
                }
            }
            BigDecimal lineDiff = linesSum.subtract(subtotal).abs();
            if (lineDiff.compareTo(ROUNDING_TOLERANCE) > 0) {
                result.addEvaluation(RuleEvaluation.warning(
                        "RULE_LINE_ITEMS_SUM_MATCH",
                        String.format("Sum of line items (%s) differs from subtotal (%s)", linesSum, subtotal),
                        "subtotal"
                ));
            } else {
                result.addEvaluation(RuleEvaluation.pass("RULE_LINE_ITEMS_SUM_MATCH", "Line items sum matches invoice subtotal"));
            }
        }

        // 3. Duplicate Detection Check
        if (invoice.getSupplierId() != null && invoice.getInvoiceNumber() != null && invoice.getCurrency() != null) {
            List<Invoice> duplicates = invoiceRepository.findPotentialDuplicates(
                    invoice.getOrganizationId(),
                    invoice.getSupplierId(),
                    invoice.getInvoiceNumber(),
                    invoice.getCurrency()
            );

            int windowDays = 90;
            if (settingsRepository != null && invoice.getOrganizationId() != null) {
                try {
                    windowDays = settingsRepository.findByOrganizationId(invoice.getOrganizationId())
                            .map(OrganizationSettings::getDuplicateWindowDays)
                            .orElse(90);
                } catch (Exception ignored) {}
            }

            LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
            LocalDate cutoff = invDate.minusDays(windowDays);

            boolean hasDuplicate = duplicates.stream()
                    .filter(d -> !d.getId().equals(invoice.getId()))
                    .anyMatch(d -> d.getInvoiceDate() == null || !d.getInvoiceDate().isBefore(cutoff));

            if (hasDuplicate) {
                result.addEvaluation(RuleEvaluation.fail(
                        "RULE_DUPLICATE_CHECK",
                        String.format("Duplicate invoice detected: supplier %s already has invoice number %s with currency %s",
                                invoice.getSupplierId(), invoice.getInvoiceNumber(), invoice.getCurrency()),
                        "invoiceNumber"
                ));
            } else {
                result.addEvaluation(RuleEvaluation.pass("RULE_DUPLICATE_CHECK", "No duplicate invoice found for this supplier and invoice number"));
            }
        }

        return result;
    }
}
