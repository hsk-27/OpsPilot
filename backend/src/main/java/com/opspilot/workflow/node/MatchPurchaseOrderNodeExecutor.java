package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.validation.dto.PoMatchResult;
import com.opspilot.validation.service.PurchaseOrderMatchingService;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

@Component
public class MatchPurchaseOrderNodeExecutor implements WorkflowNodeExecutor {

    private final InvoiceRepository invoiceRepository;
    private final PurchaseOrderMatchingService poMatchingService;
    private final ObjectMapper objectMapper;

    public MatchPurchaseOrderNodeExecutor(
            InvoiceRepository invoiceRepository,
            PurchaseOrderMatchingService poMatchingService,
            ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.poMatchingService = poMatchingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.MATCH_PURCHASE_ORDER;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        try {
            Invoice invoice = invoiceRepository.findById(context.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + context.getInvoiceId()));

            PoMatchResult matchResult = poMatchingService.matchInvoice(invoice);
            String resultJson = objectMapper.writeValueAsString(matchResult);

            context.getContextData().put("poMatchResult", matchResult);
            context.getContextData().put("poMatchOutcome", matchResult.getOutcome().name());

            return NodeExecutionResult.completed(resultJson);

        } catch (Exception ex) {
            return NodeExecutionResult.failed("PO matching failed: " + ex.getMessage());
        }
    }
}
