package com.opspilot.export.controller;

import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.export.entity.ExportRecord;
import com.opspilot.export.repository.ExportRecordRepository;
import com.opspilot.export.service.InvoiceExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
@Tag(name = "Accounting Export", description = "Export approved invoices to CSV, JSON, and ERP systems")
public class ExportController {

    private final InvoiceExportService exportService;
    private final ExportRecordRepository exportRecordRepository;

    public ExportController(InvoiceExportService exportService, ExportRecordRepository exportRecordRepository) {
        this.exportService = exportService;
        this.exportRecordRepository = exportRecordRepository;
    }

    @PostMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Export an approved invoice")
    public ResponseEntity<ApiResponse<ExportRecord>> exportInvoice(
            @PathVariable UUID invoiceId,
            @RequestBody(required = false) Map<String, String> body) {
        String format = body != null ? body.get("format") : "CSV";
        String destination = body != null ? body.get("destination") : "DOWNLOAD";
        ExportRecord record = exportService.exportInvoice(invoiceId, format, destination);
        return ResponseEntity.ok(ApiResponse.success("Invoice exported successfully", record));
    }

    @GetMapping
    @Operation(summary = "List export records for current organization")
    public ResponseEntity<ApiResponse<PagedResponse<ExportRecord>>> getExports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(exportRecordRepository.findByOrganizationId(orgId, pageable))));
    }
}
