package com.opspilot.invoice.controller;

import com.opspilot.audit.entity.AuditEvent;
import com.opspilot.audit.service.AuditService;
import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.security.UserPrincipal;
import com.opspilot.document.service.DocumentStorageService;
import com.opspilot.invoice.dto.InvoiceDetailDto;
import com.opspilot.invoice.dto.InvoiceDto;
import com.opspilot.invoice.dto.UpdateInvoiceRequest;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.service.InvoiceService;
import com.opspilot.validation.dto.ValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Invoice upload, processing, validation, and historical inspection")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final DocumentStorageService documentStorageService;
    private final AuditService auditService;

    public InvoiceController(InvoiceService invoiceService, DocumentStorageService documentStorageService, AuditService auditService) {
        this.invoiceService = invoiceService;
        this.documentStorageService = documentStorageService;
        this.auditService = auditService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Upload invoice PDF and start processing workflow")
    public ResponseEntity<ApiResponse<InvoiceDetailDto>> uploadInvoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "purchaseOrderId", required = false) UUID purchaseOrderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceDetailDto result = invoiceService.uploadInvoice(file, purchaseOrderId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Invoice uploaded and workflow triggered", result));
    }

    @GetMapping
    @Operation(summary = "List invoices for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceDto>>> getInvoices(
            @RequestParam(value = "status", required = false) ProcessingStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoices(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice aggregate details including lines and validation results")
    public ResponseEntity<ApiResponse<InvoiceDetailDto>> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoiceDetail(id)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Update invoice fields or correct extraction results")
    public ResponseEntity<ApiResponse<InvoiceDetailDto>> updateInvoice(
            @PathVariable UUID id,
            @RequestBody UpdateInvoiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Invoice updated", invoiceService.updateInvoice(id, request, principal.getId())));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Trigger deterministic business validation on demand")
    public ResponseEntity<ApiResponse<ValidationResult>> validateInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.validateInvoice(id)));
    }

    @PostMapping("/{id}/reprocess")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Reprocess invoice from extraction")
    public ResponseEntity<ApiResponse<InvoiceDetailDto>> reprocessInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Invoice reprocess started", invoiceService.reprocessInvoice(id, principal.getId())));
    }

    @GetMapping("/{id}/document")
    @Operation(summary = "Download original invoice PDF document")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id) {
        InvoiceDetailDto invoice = invoiceService.getInvoiceDetail(id);
        if (invoice.getDocumentId() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = documentStorageService.loadDocumentBytes(invoice.getDocumentId());
        ByteArrayResource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + invoice.getOriginalFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get audit trail for this invoice")
    public ResponseEntity<ApiResponse<List<AuditEvent>>> getInvoiceHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAuditTrail(id.toString())));
    }
}
