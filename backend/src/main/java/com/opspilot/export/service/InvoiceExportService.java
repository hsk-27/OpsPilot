package com.opspilot.export.service;

import com.opspilot.audit.service.AuditService;
import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.export.entity.ExportRecord;
import com.opspilot.export.provider.AccountingExportProvider;
import com.opspilot.export.repository.ExportRecordRepository;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceLineRepository;
import com.opspilot.invoice.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvoiceExportService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final ExportRecordRepository exportRecordRepository;
    private final AuditService auditService;
    private final Map<String, AccountingExportProvider> providerMap;

    public InvoiceExportService(
            InvoiceRepository invoiceRepository,
            InvoiceLineRepository invoiceLineRepository,
            ExportRecordRepository exportRecordRepository,
            AuditService auditService,
            List<AccountingExportProvider> providers) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.exportRecordRepository = exportRecordRepository;
        this.auditService = auditService;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(p -> p.getFormat().toUpperCase(), p -> p));
    }

    @Transactional
    public ExportRecord exportInvoice(UUID invoiceId, String format, String destination) {
        UUID orgId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        // Eligibility Check: Must be APPROVED
        if (invoice.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException("Invoice must be APPROVED before export. Current status: " + invoice.getApprovalStatus(),
                    HttpStatus.BAD_REQUEST, "INVOICE_NOT_APPROVED");
        }

        String formatKey = (format != null ? format : "CSV").toUpperCase();
        AccountingExportProvider provider = providerMap.get(formatKey);
        if (provider == null) {
            throw new ApiException("Unsupported export format: " + format, HttpStatus.BAD_REQUEST, "UNSUPPORTED_EXPORT_FORMAT");
        }

        String idempotencyKey = String.format("EXP-%s-%s", invoice.getId(), formatKey);
        var existingOpt = exportRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            return existingOpt.get(); // Idempotent return
        }

        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoice.getId());

        ExportRecord record = new ExportRecord(
                orgId,
                invoice.getId(),
                formatKey,
                destination != null ? destination : "LOCAL_EXPORT",
                idempotencyKey
        );

        AccountingExportProvider.ExportResult result = provider.exportInvoice(invoice, lines, destination);
        if (result.isSuccess()) {
            record.setStatus("COMPLETED");
            record.setPayloadSummary(result.getPayloadSummary());
            record.setExportedAt(Instant.now());

            invoice.setProcessingStatus(ProcessingStatus.EXPORTED);
            invoiceRepository.save(invoice);

            auditService.recordSystemEvent(orgId, "INVOICE_EXPORTED", "INVOICE", invoice.getId().toString(),
                    "PENDING_EXPORT", "EXPORTED");
        } else {
            record.setStatus("FAILED");
            record.setErrorMessage(result.getErrorMessage());
        }

        return exportRecordRepository.save(record);
    }
}
