package com.opspilot.purchaseorder.controller;

import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.purchaseorder.dto.CreatePurchaseOrderRequest;
import com.opspilot.purchaseorder.dto.PurchaseOrderDto;
import com.opspilot.purchaseorder.dto.UpdatePurchaseOrderRequest;
import com.opspilot.purchaseorder.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@Tag(name = "Purchase Orders", description = "Purchase order registry, lines, and status tracking")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    @Operation(summary = "List purchase orders for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<PurchaseOrderDto>>> getPurchaseOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.getPurchaseOrders(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order details and line items")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getPurchaseOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.getPurchaseOrder(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Create a new purchase order with lines")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order created successfully", purchaseOrderService.createPurchaseOrder(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Update purchase order status, totals, or supplier")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> updatePurchaseOrder(
            @PathVariable UUID id,
            @RequestBody UpdatePurchaseOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order updated successfully", purchaseOrderService.updatePurchaseOrder(id, request)));
    }
}
