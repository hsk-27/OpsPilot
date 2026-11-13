package com.opspilot.validation;

import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.entity.ValidationStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.validation.dto.ValidationResult;
import com.opspilot.validation.service.DeterministicValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicValidationServiceTest {

    private InvoiceRepository invoiceRepository;
    private SupplierRepository supplierRepository;
    private DeterministicValidationService validationService;
    private List<Invoice> duplicateReturnList;
    private Supplier activeSupplier;

    @BeforeEach
    void setUp() {
        duplicateReturnList = new ArrayList<>();
        activeSupplier = null;

        invoiceRepository = (InvoiceRepository) Proxy.newProxyInstance(
                InvoiceRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceRepository.class},
                (proxy, method, args) -> {
                    if ("findPotentialDuplicates".equals(method.getName())) {
                        return duplicateReturnList;
                    }
                    return null;
                }
        );

        supplierRepository = (SupplierRepository) Proxy.newProxyInstance(
                SupplierRepository.class.getClassLoader(),
                new Class<?>[]{SupplierRepository.class},
                (proxy, method, args) -> {
                    if ("findByIdAndOrganizationId".equals(method.getName())) {
                        return Optional.ofNullable(activeSupplier);
                    }
                    return Optional.empty();
                }
        );

        validationService = new DeterministicValidationService(invoiceRepository, supplierRepository);
    }

    @Test
    @DisplayName("Valid invoice calculation passes all deterministic rules")
    void testValidInvoicePasses() {
        UUID orgId = UUID.randomUUID();
        UUID supId = UUID.randomUUID();
        Supplier sup = new Supplier(orgId, "SUP-1", "Acme", null, null, null, "USD");
        sup.setId(supId);
        sup.setStatus("ACTIVE");
        activeSupplier = sup;

        Invoice invoice = new Invoice(orgId, "INV-1001", null);
        invoice.setSupplierId(supId);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setCurrency("USD");
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(new BigDecimal("18.00"));
        invoice.setTotalAmount(new BigDecimal("118.00"));
        invoice.addLine(new InvoiceLine("Item A", BigDecimal.ONE, new BigDecimal("100.00"), new BigDecimal("18.00"), new BigDecimal("100.00")));

        ValidationResult result = validationService.validateInvoice(invoice);

        assertEquals(ValidationStatus.PASSED, result.getOverallStatus());
        assertEquals(0, result.getFailedRules());
        assertTrue(result.getPassedRules() >= 4);
    }

    @Test
    @DisplayName("Arithmetic mismatch between subtotal, tax, and total fails validation")
    void testMathMismatchFails() {
        UUID orgId = UUID.randomUUID();
        Invoice invoice = new Invoice(orgId, "INV-1002", null);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setCurrency("USD");
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(new BigDecimal("18.00"));
        invoice.setTotalAmount(new BigDecimal("200.00")); // Stated 200 vs expected 118

        ValidationResult result = validationService.validateInvoice(invoice);

        assertEquals(ValidationStatus.FAILED, result.getOverallStatus());
        assertTrue(result.getFailedRules() > 0);
        assertTrue(result.getReviewReasons().stream().anyMatch(r -> r.contains("differs from stated total")));
    }

    @Test
    @DisplayName("Negative total amount is strictly rejected")
    void testNegativeTotalFails() {
        UUID orgId = UUID.randomUUID();
        Invoice invoice = new Invoice(orgId, "INV-1003", null);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setCurrency("USD");
        invoice.setSubtotal(new BigDecimal("-50.00"));
        invoice.setTotalAmount(new BigDecimal("-50.00"));

        ValidationResult result = validationService.validateInvoice(invoice);

        assertEquals(ValidationStatus.FAILED, result.getOverallStatus());
        assertTrue(result.getReviewReasons().stream().anyMatch(r -> r.contains("Subtotal cannot be negative") || r.contains("greater than zero")));
    }

    @Test
    @DisplayName("Duplicate invoice detected for same supplier and invoice number")
    void testDuplicateInvoiceDetected() {
        UUID orgId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        Supplier sup = new Supplier(orgId, "SUP-1", "Acme", null, null, null, "USD");
        sup.setId(supplierId);
        activeSupplier = sup;

        Invoice currentInvoice = new Invoice(orgId, "INV-9999", null);
        currentInvoice.setId(UUID.randomUUID());
        currentInvoice.setSupplierId(supplierId);
        currentInvoice.setInvoiceDate(LocalDate.now());
        currentInvoice.setCurrency("USD");
        currentInvoice.setSubtotal(new BigDecimal("100.00"));
        currentInvoice.setTotalAmount(new BigDecimal("100.00"));

        Invoice existingDuplicate = new Invoice(orgId, "INV-9999", null);
        existingDuplicate.setId(UUID.randomUUID()); // different ID

        duplicateReturnList.add(existingDuplicate);

        ValidationResult result = validationService.validateInvoice(currentInvoice);

        assertEquals(ValidationStatus.FAILED, result.getOverallStatus());
        assertTrue(result.getReviewReasons().stream().anyMatch(r -> r.contains("Duplicate invoice detected")));
    }

    @Test
    @DisplayName("Due date preceding invoice issue date fails validation")
    void testDueDatePrecedingInvoiceDateFails() {
        UUID orgId = UUID.randomUUID();
        Invoice invoice = new Invoice(orgId, "INV-2001", null);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().minusDays(10));
        invoice.setCurrency("USD");
        invoice.setSubtotal(new BigDecimal("500.00"));
        invoice.setTotalAmount(new BigDecimal("500.00"));

        ValidationResult result = validationService.validateInvoice(invoice);

        assertEquals(ValidationStatus.FAILED, result.getOverallStatus());
        assertTrue(result.getReviewReasons().stream().anyMatch(r -> r.contains("cannot precede invoice date")));
    }

    @Test
    @DisplayName("Invoice date beyond 30 days in the future is rejected")
    void testExtremeFutureInvoiceDateFails() {
        UUID orgId = UUID.randomUUID();
        Invoice invoice = new Invoice(orgId, "INV-2002", null);
        invoice.setInvoiceDate(LocalDate.now().plusDays(45));
        invoice.setCurrency("USD");
        invoice.setSubtotal(new BigDecimal("300.00"));
        invoice.setTotalAmount(new BigDecimal("300.00"));

        ValidationResult result = validationService.validateInvoice(invoice);

        assertEquals(ValidationStatus.FAILED, result.getOverallStatus());
        assertTrue(result.getReviewReasons().stream().anyMatch(r -> r.contains("exceeds permissible 30-day future")));
    }

    @Test
    @DisplayName("Line item arithmetic discrepancy produces warning")
    void testLineItemArithmeticDiscrepancyProducesWarning() {
        UUID orgId = UUID.randomUUID();
        Invoice invoice = new Invoice(orgId, "INV-2003", null);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setCurrency("USD");
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTotalAmount(new BigDecimal("100.00"));

        // Quantity 2 * UnitPrice 40 = 80, but stated line total is 100
        invoice.addLine(new InvoiceLine("Hardware Component", new BigDecimal("2"), new BigDecimal("40.00"), BigDecimal.ZERO, new BigDecimal("100.00")));

        ValidationResult result = validationService.validateInvoice(invoice);

        assertEquals(ValidationStatus.WARNING, result.getOverallStatus());
        assertTrue(result.getWarningsCount() > 0);
        assertTrue(result.getReviewReasons().stream().anyMatch(r -> r.contains("arithmetic variance")));
    }
}
