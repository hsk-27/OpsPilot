package com.opspilot.validation;

import com.opspilot.invoice.entity.Invoice;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
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
}
