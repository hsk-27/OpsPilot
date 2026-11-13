package com.opspilot.validation;

import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.entity.PurchaseOrderLine;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.validation.dto.LineMatchResult;
import com.opspilot.validation.dto.PoMatchResult;
import com.opspilot.validation.service.PurchaseOrderMatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderMatchingServiceTest {

    private PurchaseOrderRepository purchaseOrderRepository;
    private PurchaseOrderMatchingService matchingService;
    private PurchaseOrder returnedPo;

    @BeforeEach
    void setUp() {
        returnedPo = null;
        purchaseOrderRepository = (PurchaseOrderRepository) Proxy.newProxyInstance(
                PurchaseOrderRepository.class.getClassLoader(),
                new Class<?>[]{PurchaseOrderRepository.class},
                (proxy, method, args) -> {
                    if ("findByIdAndOrganizationId".equals(method.getName())) {
                        return Optional.ofNullable(returnedPo);
                    }
                    return null;
                }
        );
        matchingService = new PurchaseOrderMatchingService(purchaseOrderRepository);
    }

    @Test
    @DisplayName("PO match succeeds when supplier, currency, and total match within tolerance")
    void testSuccessfulMatch() {
        UUID orgId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID poId = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder(orgId, supplierId, "PO-100", LocalDate.now(), "USD");
        po.setTotalAmount(new BigDecimal("1000.00"));
        returnedPo = po;

        Invoice inv = new Invoice(orgId, "INV-100", null);
        inv.setSupplierId(supplierId);
        inv.setPurchaseOrderId(poId);
        inv.setCurrency("USD");
        inv.setTotalAmount(new BigDecimal("1000.00"));

        PoMatchResult result = matchingService.matchInvoice(inv);

        assertEquals(PoMatchResult.Outcome.MATCHED, result.getOutcome());
        assertTrue(result.isSupplierMatches());
        assertTrue(result.isCurrencyMatches());
    }

    @Test
    @DisplayName("Supplier mismatch between PO and invoice flags MISMATCHED")
    void testSupplierMismatch() {
        UUID orgId = UUID.randomUUID();
        UUID poId = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder(orgId, UUID.randomUUID(), "PO-100", LocalDate.now(), "USD");
        po.setTotalAmount(new BigDecimal("1000.00"));
        returnedPo = po;

        Invoice inv = new Invoice(orgId, "INV-100", null);
        inv.setSupplierId(UUID.randomUUID()); // Different supplier
        inv.setPurchaseOrderId(poId);
        inv.setCurrency("USD");
        inv.setTotalAmount(new BigDecimal("1000.00"));

        PoMatchResult result = matchingService.matchInvoice(inv);

        assertEquals(PoMatchResult.Outcome.MISMATCHED, result.getOutcome());
        assertFalse(result.isSupplierMatches());
    }

    @Test
    @DisplayName("Invoice with no PO reference returns NO_PURCHASE_ORDER")
    void testNoPurchaseOrder() {
        UUID orgId = UUID.randomUUID();
        Invoice inv = new Invoice(orgId, "INV-100", null);

        PoMatchResult result = matchingService.matchInvoice(inv);

        assertEquals(PoMatchResult.Outcome.NO_PURCHASE_ORDER, result.getOutcome());
    }

    @Test
    @DisplayName("Line-level exact match validates all items, quantities, and rates")
    void testLineLevelExactMatch() {
        UUID orgId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID poId = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder(orgId, supplierId, "PO-200", LocalDate.now(), "USD");
        po.setTotalAmount(new BigDecimal("1200.00"));
        po.addLine(new PurchaseOrderLine("Cloud Compute", new BigDecimal("2"), new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00")));
        po.addLine(new PurchaseOrderLine("Storage Tier", new BigDecimal("1"), new BigDecimal("200.00"), BigDecimal.ZERO, new BigDecimal("200.00")));
        returnedPo = po;

        Invoice inv = new Invoice(orgId, "INV-200", null);
        inv.setSupplierId(supplierId);
        inv.setPurchaseOrderId(poId);
        inv.setCurrency("USD");
        inv.setTotalAmount(new BigDecimal("1200.00"));
        inv.addLine(new InvoiceLine("Cloud Compute", new BigDecimal("2"), new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("1000.00")));
        inv.addLine(new InvoiceLine("Storage Tier", new BigDecimal("1"), new BigDecimal("200.00"), BigDecimal.ZERO, new BigDecimal("200.00")));

        PoMatchResult result = matchingService.matchInvoice(inv);

        assertEquals(PoMatchResult.Outcome.MATCHED, result.getOutcome());
        assertEquals(2, result.getMatchedLinesCount());
        assertEquals(2, result.getTotalLinesCount());
        assertEquals(2, result.getLineMatches().size());
        assertTrue(result.getLineMatches().stream().allMatch(LineMatchResult::isMatched));
        assertEquals("EXACT", result.getLineMatches().get(0).getMatchType());
    }

    @Test
    @DisplayName("Line-level quantity variance flags discrepancy")
    void testLineLevelQuantityVariance() {
        UUID orgId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID poId = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder(orgId, supplierId, "PO-300", LocalDate.now(), "USD");
        po.setTotalAmount(new BigDecimal("1000.00"));
        po.addLine(new PurchaseOrderLine("Consulting Hours", new BigDecimal("10"), new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00")));
        returnedPo = po;

        Invoice inv = new Invoice(orgId, "INV-300", null);
        inv.setSupplierId(supplierId);
        inv.setPurchaseOrderId(poId);
        inv.setCurrency("USD");
        inv.setTotalAmount(new BigDecimal("1000.00"));
        // Billed 15 hours instead of authorized 10
        inv.addLine(new InvoiceLine("Consulting Hours", new BigDecimal("15"), new BigDecimal("66.66"), BigDecimal.ZERO, new BigDecimal("1000.00")));

        PoMatchResult result = matchingService.matchInvoice(inv);

        assertEquals(PoMatchResult.Outcome.PARTIALLY_MATCHED, result.getOutcome());
        assertEquals(0, result.getMatchedLinesCount());
        assertEquals("QUANTITY_VARIANCE", result.getLineMatches().get(0).getMatchType());
        assertFalse(result.getLineMatches().get(0).isMatched());
    }

    @Test
    @DisplayName("Unmatched line item not on PO flags discrepancy")
    void testUnmatchedInvoiceLine() {
        UUID orgId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        UUID poId = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder(orgId, supplierId, "PO-400", LocalDate.now(), "USD");
        po.setTotalAmount(new BigDecimal("500.00"));
        po.addLine(new PurchaseOrderLine("Design Service", new BigDecimal("1"), new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("500.00")));
        returnedPo = po;

        Invoice inv = new Invoice(orgId, "INV-400", null);
        inv.setSupplierId(supplierId);
        inv.setPurchaseOrderId(poId);
        inv.setCurrency("USD");
        inv.setTotalAmount(new BigDecimal("500.00"));
        inv.addLine(new InvoiceLine("Expedited Shipping Fee", new BigDecimal("1"), new BigDecimal("500.00"), BigDecimal.ZERO, new BigDecimal("500.00")));

        PoMatchResult result = matchingService.matchInvoice(inv);

        assertEquals(PoMatchResult.Outcome.PARTIALLY_MATCHED, result.getOutcome());
        assertTrue(result.getLineMatches().stream().anyMatch(l -> "UNMATCHED".equals(l.getMatchType()) && !l.isMatched()));
    }
}

