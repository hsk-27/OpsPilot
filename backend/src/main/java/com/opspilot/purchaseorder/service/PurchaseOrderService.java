package com.opspilot.purchaseorder.service;

import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.purchaseorder.dto.CreatePurchaseOrderRequest;
import com.opspilot.purchaseorder.dto.PurchaseOrderDto;
import com.opspilot.purchaseorder.dto.UpdatePurchaseOrderRequest;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.entity.PurchaseOrderLine;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.supplier.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository, SupplierRepository supplierRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PurchaseOrderDto> getPurchaseOrders(Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        Page<PurchaseOrder> page = purchaseOrderRepository.findByOrganizationId(orgId, pageable);
        return PagedResponse.from(page.map(PurchaseOrderDto::from));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrder(UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        PurchaseOrder po = purchaseOrderRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));
        return PurchaseOrderDto.from(po);
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePurchaseOrderRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        String poNumber = request.getPurchaseOrderNumber().trim();

        if (purchaseOrderRepository.existsByOrganizationIdAndPurchaseOrderNumber(orgId, poNumber)) {
            throw new ApiException("Purchase order number '" + poNumber + "' already exists", HttpStatus.CONFLICT, "DUPLICATE_PO_NUMBER");
        }

        if (request.getSupplierId() != null) {
            supplierRepository.findByIdAndOrganizationId(request.getSupplierId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", request.getSupplierId()));
        }

        PurchaseOrder po = new PurchaseOrder(
                orgId,
                request.getSupplierId(),
                poNumber,
                request.getIssueDate(),
                request.getCurrency()
        );

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        if (request.getLines() != null) {
            for (var lineReq : request.getLines()) {
                BigDecimal lineAmount = lineReq.getQuantity().multiply(lineReq.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineTax = lineReq.getTaxRate() != null && lineReq.getTaxRate().compareTo(BigDecimal.ZERO) > 0
                        ? lineAmount.multiply(lineReq.getTaxRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                subtotal = subtotal.add(lineAmount);
                taxTotal = taxTotal.add(lineTax);

                PurchaseOrderLine line = new PurchaseOrderLine(
                        lineReq.getDescription(),
                        lineReq.getQuantity(),
                        lineReq.getUnitPrice(),
                        lineReq.getTaxRate(),
                        lineAmount
                );
                po.addLine(line);
            }
        }

        po.setSubtotal(subtotal);
        po.setTaxAmount(taxTotal);
        po.setTotalAmount(subtotal.add(taxTotal));

        po = purchaseOrderRepository.save(po);
        return PurchaseOrderDto.from(po);
    }

    @Transactional
    public PurchaseOrderDto updatePurchaseOrder(UUID id, UpdatePurchaseOrderRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        PurchaseOrder po = purchaseOrderRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            po.setStatus(request.getStatus().trim().toUpperCase());
        }
        if (request.getTotalAmount() != null) {
            po.setTotalAmount(request.getTotalAmount());
        }
        if (request.getSupplierId() != null) {
            supplierRepository.findByIdAndOrganizationId(request.getSupplierId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", request.getSupplierId()));
            po.setSupplierId(request.getSupplierId());
        }
        po.setUpdatedAt(Instant.now());
        po = purchaseOrderRepository.save(po);
        return PurchaseOrderDto.from(po);
    }
}
