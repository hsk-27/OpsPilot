package com.opspilot.supplier.service;

import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.supplier.dto.CreateSupplierRequest;
import com.opspilot.supplier.dto.SupplierDto;
import com.opspilot.supplier.dto.UpdateSupplierRequest;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<SupplierDto> getSuppliers(Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        Page<Supplier> page = supplierRepository.findByOrganizationId(orgId, pageable);
        return PagedResponse.from(page.map(SupplierDto::from));
    }

    @Transactional(readOnly = true)
    public SupplierDto getSupplier(UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        Supplier supplier = supplierRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        return SupplierDto.from(supplier);
    }

    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        String code = request.getSupplierCode().trim();

        if (supplierRepository.existsByOrganizationIdAndSupplierCode(orgId, code)) {
            throw new ApiException("Supplier code '" + code + "' already exists in this organization",
                    HttpStatus.CONFLICT, "DUPLICATE_SUPPLIER_CODE");
        }

        Supplier supplier = new Supplier(
                orgId,
                code,
                request.getName(),
                request.getTaxIdentifier(),
                request.getEmail(),
                request.getAddress(),
                request.getDefaultCurrency()
        );

        supplier = supplierRepository.save(supplier);
        return SupplierDto.from(supplier);
    }

    @Transactional
    public SupplierDto updateSupplier(UUID id, UpdateSupplierRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        Supplier supplier = supplierRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            supplier.setName(request.getName().trim());
        }
        if (request.getTaxIdentifier() != null) {
            supplier.setTaxIdentifier(request.getTaxIdentifier().trim());
        }
        if (request.getEmail() != null) {
            supplier.setEmail(request.getEmail().trim());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress().trim());
        }
        if (request.getDefaultCurrency() != null && !request.getDefaultCurrency().isBlank()) {
            supplier.setDefaultCurrency(request.getDefaultCurrency().trim().toUpperCase());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            supplier.setStatus(request.getStatus().trim().toUpperCase());
        }

        supplier = supplierRepository.save(supplier);
        return SupplierDto.from(supplier);
    }

    @Transactional
    public void deactivateSupplier(UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        Supplier supplier = supplierRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        supplier.setStatus("INACTIVE");
        supplierRepository.save(supplier);
    }
}
