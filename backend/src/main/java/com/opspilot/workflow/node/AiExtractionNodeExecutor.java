package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.ai.provider.InvoiceExtractionRequest;
import com.opspilot.ai.provider.InvoiceExtractionResult;
import com.opspilot.ai.service.AiExtractionService;
import com.opspilot.document.service.DocumentStorageService;
import com.opspilot.document.service.DocumentTextExtractor;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceLineRepository;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
public class AiExtractionNodeExecutor implements WorkflowNodeExecutor {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final SupplierRepository supplierRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentTextExtractor textExtractor;
    private final AiExtractionService aiExtractionService;
    private final ObjectMapper objectMapper;

    public AiExtractionNodeExecutor(
            InvoiceRepository invoiceRepository,
            InvoiceLineRepository invoiceLineRepository,
            SupplierRepository supplierRepository,
            DocumentStorageService documentStorageService,
            DocumentTextExtractor textExtractor,
            AiExtractionService aiExtractionService,
            ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.supplierRepository = supplierRepository;
        this.documentStorageService = documentStorageService;
        this.textExtractor = textExtractor;
        this.aiExtractionService = aiExtractionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.AI_EXTRACTION;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        try {
            Invoice invoice = invoiceRepository.findById(context.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + context.getInvoiceId()));

            String documentText = "";
            String filename = "invoice.pdf";
            if (invoice.getDocumentId() != null) {
                Path docPath = documentStorageService.getDocumentPath(invoice.getDocumentId());
                filename = docPath.getFileName().toString();
                DocumentTextExtractor.ExtractedDocumentText extracted = textExtractor.extract(docPath);
                documentText = extracted.getText();
            }

            InvoiceExtractionRequest req = new InvoiceExtractionRequest(
                    documentText,
                    filename,
                    context.getOrganizationId(),
                    invoice.getId()
            );

            InvoiceExtractionResult extraction = aiExtractionService.extractInvoice(req);

            if (!extraction.isSuccess()) {
                invoice.setProcessingStatus(ProcessingStatus.FAILED);
                invoiceRepository.save(invoice);
                return NodeExecutionResult.failed("Extraction failed: " + extraction.getErrorMessage());
            }

            // Update invoice with extracted values
            if (extraction.getInvoiceNumber() != null && !extraction.getInvoiceNumber().isBlank()) {
                invoice.setInvoiceNumber(extraction.getInvoiceNumber());
            }
            if (extraction.getInvoiceDate() != null) {
                invoice.setInvoiceDate(extraction.getInvoiceDate());
            }
            if (extraction.getDueDate() != null) {
                invoice.setDueDate(extraction.getDueDate());
            }
            if (extraction.getCurrency() != null) {
                invoice.setCurrency(extraction.getCurrency());
            }
            if (extraction.getSubtotal() != null) {
                invoice.setSubtotal(extraction.getSubtotal());
            }
            if (extraction.getTaxAmount() != null) {
                invoice.setTaxAmount(extraction.getTaxAmount());
            }
            if (extraction.getTotalAmount() != null) {
                invoice.setTotalAmount(extraction.getTotalAmount());
            }

            // Auto-resolve supplier by name if available
            if (invoice.getSupplierId() == null && extraction.getSupplierName() != null) {
                Optional<Supplier> supOpt = supplierRepository.findByOrganizationIdAndNameIgnoreCase(
                        context.getOrganizationId(), extraction.getSupplierName()
                );
                supOpt.ifPresent(supplier -> invoice.setSupplierId(supplier.getId()));
            }

            // Populate lines if provided
            if (extraction.getLineItems() != null && !extraction.getLineItems().isEmpty()) {
                invoiceLineRepository.deleteAll(invoiceLineRepository.findByInvoiceId(invoice.getId()));
                invoice.clearLines();
                for (var lineItem : extraction.getLineItems()) {
                    InvoiceLine line = new InvoiceLine(
                            lineItem.getDescription(),
                            lineItem.getQuantity(),
                            lineItem.getUnitPrice(),
                            lineItem.getTaxRate(),
                            lineItem.getLineTotal()
                    );
                    invoice.addLine(line);
                }
            }

            String extractionJson = objectMapper.writeValueAsString(extraction);
            invoice.setExtractedData(extractionJson);
            invoice.setProcessingStatus(ProcessingStatus.EXTRACTED);
            invoiceRepository.save(invoice);

            context.getContextData().put("extraction", extraction);
            context.getContextData().put("averageConfidence", extraction.getAverageConfidence());

            return NodeExecutionResult.completed(extractionJson);

        } catch (Exception ex) {
            return NodeExecutionResult.failed("AI extraction execution failed: " + ex.getMessage());
        }
    }
}
