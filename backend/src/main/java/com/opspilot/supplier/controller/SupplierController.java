package com.opspilot.supplier.controller;

import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.supplier.dto.CreateSupplierRequest;
import com.opspilot.supplier.dto.SupplierDto;
import com.opspilot.supplier.dto.UpdateSupplierRequest;
import com.opspilot.supplier.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Suppliers", description = "Supplier directory, onboarding, and status management")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @Operation(summary = "List suppliers for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<SupplierDto>>> getSuppliers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.getSuppliers(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier details")
    public ResponseEntity<ApiResponse<SupplierDto>> getSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.getSupplier(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Create a new supplier")
    public ResponseEntity<ApiResponse<SupplierDto>> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier created successfully", supplierService.createSupplier(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Update supplier details")
    public ResponseEntity<ApiResponse<SupplierDto>> updateSupplier(
            @PathVariable UUID id,
            @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Supplier updated successfully", supplierService.updateSupplier(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Deactivate supplier")
    public ResponseEntity<ApiResponse<Void>> deactivateSupplier(@PathVariable UUID id) {
        supplierService.deactivateSupplier(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier deactivated", null));
    }
}
