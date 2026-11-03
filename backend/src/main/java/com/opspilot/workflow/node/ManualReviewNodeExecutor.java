package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

@Component
public class ManualReviewNodeExecutor implements WorkflowNodeExecutor {

    private final InvoiceRepository invoiceRepository;

    public ManualReviewNodeExecutor(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.MANUAL_REVIEW;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        Invoice invoice = invoiceRepository.findById(context.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + context.getInvoiceId()));

        invoice.setProcessingStatus(ProcessingStatus.NEEDS_REVIEW);
        invoiceRepository.save(invoice);

        String snapshot = "{\"status\":\"NEEDS_REVIEW\",\"message\":\"Paused for AP operator manual correction or exception review\"}";
        return NodeExecutionResult.waitingApproval(snapshot);
    }
}
