package com.opspilot.supplier;

import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.supplier.dto.CreateSupplierRequest;
import com.opspilot.supplier.dto.SupplierDto;
import com.opspilot.supplier.dto.UpdateSupplierRequest;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.supplier.service.SupplierService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SupplierServiceTest {

    private SupplierService supplierService;
    private final UUID tenantId = UUID.randomUUID();
    private final Map<UUID, Supplier> store = new HashMap<>();

    @BeforeEach
    public void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        store.clear();

        SupplierRepository repo = (SupplierRepository) Proxy.newProxyInstance(
                SupplierRepository.class.getClassLoader(),
                new Class<?>[]{SupplierRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Supplier s = (Supplier) args[0];
                        if (s.getId() == null) s.setId(UUID.randomUUID());
                        store.put(s.getId(), s);
                        return s;
                    }
                    if ("findByIdAndOrganizationId".equals(name)) {
                        UUID id = (UUID) args[0];
                        UUID orgId = (UUID) args[1];
                        Supplier s = store.get(id);
                        if (s != null && orgId.equals(s.getOrganizationId())) {
                            return Optional.of(s);
                        }
                        return Optional.empty();
                    }
                    if ("existsByOrganizationIdAndSupplierCode".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        String code = (String) args[1];
                        return store.values().stream()
                                .anyMatch(s -> orgId.equals(s.getOrganizationId()) && code.equalsIgnoreCase(s.getSupplierCode()));
                    }
                    if ("findByOrganizationId".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        List<Supplier> list = store.values().stream()
                                .filter(s -> orgId.equals(s.getOrganizationId()))
                                .toList();
                        return new PageImpl<>(list);
                    }
                    return null;
                }
        );

        supplierService = new SupplierService(repo);
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create supplier successfully")
    public void testCreateSupplier() {
        CreateSupplierRequest req = new CreateSupplierRequest();
        req.setSupplierCode("SUP-100");
        req.setName("Dunder Mifflin");
        req.setEmail("orders@dundermifflin.com");
        req.setDefaultCurrency("USD");

        SupplierDto dto = supplierService.createSupplier(req);
        assertNotNull(dto.getId());
        assertEquals("SUP-100", dto.getSupplierCode());
        assertEquals("Dunder Mifflin", dto.getName());
    }

    @Test
    @DisplayName("Create duplicate supplier code throws 409 CONFLICT")
    public void testDuplicateSupplierCode() {
        CreateSupplierRequest req1 = new CreateSupplierRequest();
        req1.setSupplierCode("SUP-DUPE");
        req1.setName("Supplier A");
        supplierService.createSupplier(req1);

        CreateSupplierRequest req2 = new CreateSupplierRequest();
        req2.setSupplierCode("SUP-DUPE");
        req2.setName("Supplier B");

        ApiException ex = assertThrows(ApiException.class, () -> supplierService.createSupplier(req2));
        assertEquals("DUPLICATE_SUPPLIER_CODE", ex.getCode());
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    @DisplayName("Update supplier name and address")
    public void testUpdateSupplier() {
        CreateSupplierRequest req = new CreateSupplierRequest();
        req.setSupplierCode("SUP-200");
        req.setName("Vance Refrigeration");
        SupplierDto created = supplierService.createSupplier(req);

        UpdateSupplierRequest updateReq = new UpdateSupplierRequest();
        updateReq.setName("Vance Refrigeration LLC");
        updateReq.setAddress("Scranton Business Park");

        SupplierDto updated = supplierService.updateSupplier(created.getId(), updateReq);
        assertEquals("Vance Refrigeration LLC", updated.getName());
        assertEquals("Scranton Business Park", updated.getAddress());
    }
}
