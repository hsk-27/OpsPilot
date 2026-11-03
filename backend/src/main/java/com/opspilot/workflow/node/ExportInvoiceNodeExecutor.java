package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.export.entity.ExportRecord;
import com.opspilot.export.service.InvoiceExportService;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

@Component
public class ExportInvoiceNodeExecutor implements WorkflowNodeExecutor {

    private final InvoiceExportService exportService;
    private final InvoiceRepository invoiceRepository;

    public ExportInvoiceNodeExecutor(InvoiceExportService exportService, InvoiceRepository invoiceRepository) {
        this.exportService = exportService;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.EXPORT_INVOICE;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        Invoice invoice = invoiceRepository.findById(context.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + context.getInvoiceId()));

        if (invoice.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return NodeExecutionResult.failed("Cannot export unapproved invoice (status: " + invoice.getApprovalStatus() + ")");
        }

        String format = configuration != null && configuration.has("format")
                ? configuration.get("format").asText("CSV")
                : "CSV";

        ExportRecord record = exportService.exportInvoice(invoice.getId(), format, "AUTO_PIPELINE");
        String snapshot = String.format("{\"exportId\":\"%s\",\"format\":\"%s\",\"status\":\"%s\"}",
                record.getId(), record.getExportFormat(), record.getStatus());

        return NodeExecutionResult.completed(snapshot);
    }
}
