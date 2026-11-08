package com.opspilot.purchaseorder;

import com.opspilot.common.exception.ApiException;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.purchaseorder.dto.*;
import com.opspilot.purchaseorder.dto.CreatePurchaseOrderRequest.CreatePoLineRequest;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.purchaseorder.service.PurchaseOrderService;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PurchaseOrderServiceTest {

    private PurchaseOrderService purchaseOrderService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID supplierId = UUID.randomUUID();
    private final Map<UUID, PurchaseOrder> poStore = new HashMap<>();
    private final Map<UUID, Supplier> supplierStore = new HashMap<>();

    @BeforeEach
    public void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        poStore.clear();
        supplierStore.clear();

        Supplier s = new Supplier(tenantId, "SUP-01", "Acme Supplies");
        s.setId(supplierId);
        supplierStore.put(supplierId, s);

        PurchaseOrderRepository poRepo = (PurchaseOrderRepository) Proxy.newProxyInstance(
                PurchaseOrderRepository.class.getClassLoader(),
                new Class<?>[]{PurchaseOrderRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        PurchaseOrder po = (PurchaseOrder) args[0];
                        if (po.getId() == null) po.setId(UUID.randomUUID());
                        poStore.put(po.getId(), po);
                        return po;
                    }
                    if ("findByIdAndOrganizationId".equals(name)) {
                        UUID id = (UUID) args[0];
                        UUID orgId = (UUID) args[1];
                        PurchaseOrder po = poStore.get(id);
                        if (po != null && orgId.equals(po.getOrganizationId())) {
                            return Optional.of(po);
                        }
                        return Optional.empty();
                    }
                    if ("existsByOrganizationIdAndPurchaseOrderNumber".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        String num = (String) args[1];
                        return poStore.values().stream()
                                .anyMatch(po -> orgId.equals(po.getOrganizationId()) && num.equalsIgnoreCase(po.getPurchaseOrderNumber()));
                    }
                    if ("findByOrganizationId".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        List<PurchaseOrder> list = poStore.values().stream()
                                .filter(po -> orgId.equals(po.getOrganizationId()))
                                .toList();
                        return new PageImpl<>(list);
                    }
                    return null;
                }
        );

        SupplierRepository supRepo = (SupplierRepository) Proxy.newProxyInstance(
                SupplierRepository.class.getClassLoader(),
                new Class<?>[]{SupplierRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByIdAndOrganizationId".equals(name)) {
                        UUID id = (UUID) args[0];
                        return Optional.ofNullable(supplierStore.get(id));
                    }
                    return null;
                }
        );

        purchaseOrderService = new PurchaseOrderService(poRepo, supRepo);
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create purchase order with lines and verify total calculations")
    public void testCreatePurchaseOrder() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
        req.setPurchaseOrderNumber("PO-2026-001");
        req.setSupplierId(supplierId);
        req.setIssueDate(LocalDate.now());
        req.setCurrency("USD");

        CreatePoLineRequest line1 = new CreatePoLineRequest();
        line1.setDescription("Heavy Duty Steel Beam");
        line1.setQuantity(new BigDecimal("10.0000"));
        line1.setUnitPrice(new BigDecimal("200.00"));
        line1.setTaxRate(new BigDecimal("10.00")); // 10% tax = $200

        CreatePoLineRequest line2 = new CreatePoLineRequest();
        line2.setDescription("Industrial Fasteners Pack");
        line2.setQuantity(new BigDecimal("5.0000"));
        line2.setUnitPrice(new BigDecimal("100.00"));
        line2.setTaxRate(new BigDecimal("0.00"));

        req.setLines(List.of(line1, line2));

        PurchaseOrderDto dto = purchaseOrderService.createPurchaseOrder(req);
        assertNotNull(dto.getId());
        assertEquals("PO-2026-001", dto.getPurchaseOrderNumber());
        assertEquals(new BigDecimal("2500.00"), dto.getSubtotal()); // 2000 + 500
        assertEquals(new BigDecimal("200.00"), dto.getTaxAmount()); // 200 + 0
        assertEquals(new BigDecimal("2700.00"), dto.getTotalAmount()); // 2700
    }

    @Test
    @DisplayName("Create duplicate purchase order number throws 409 CONFLICT")
    public void testDuplicatePoNumber() {
        CreatePurchaseOrderRequest req1 = new CreatePurchaseOrderRequest();
        req1.setPurchaseOrderNumber("PO-DUPE");
        req1.setLines(List.of());
        purchaseOrderService.createPurchaseOrder(req1);

        CreatePurchaseOrderRequest req2 = new CreatePurchaseOrderRequest();
        req2.setPurchaseOrderNumber("PO-DUPE");
        req2.setLines(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> purchaseOrderService.createPurchaseOrder(req2));
        assertEquals("DUPLICATE_PO_NUMBER", ex.getCode());
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    @DisplayName("Update purchase order status and total amount")
    public void testUpdatePurchaseOrder() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest();
        req.setPurchaseOrderNumber("PO-UPDATE");
        req.setLines(List.of());
        PurchaseOrderDto created = purchaseOrderService.createPurchaseOrder(req);

        UpdatePurchaseOrderRequest updateReq = new UpdatePurchaseOrderRequest();
        updateReq.setStatus("CLOSED");
        updateReq.setTotalAmount(new BigDecimal("3500.00"));

        PurchaseOrderDto updated = purchaseOrderService.updatePurchaseOrder(created.getId(), updateReq);
        assertEquals("CLOSED", updated.getStatus());
        assertEquals(new BigDecimal("3500.00"), updated.getTotalAmount());
    }
}
