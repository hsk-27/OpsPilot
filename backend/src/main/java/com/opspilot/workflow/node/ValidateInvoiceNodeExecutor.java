package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.entity.ValidationStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.validation.dto.ValidationResult;
import com.opspilot.validation.service.DeterministicValidationService;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

@Component
public class ValidateInvoiceNodeExecutor implements WorkflowNodeExecutor {

    private final InvoiceRepository invoiceRepository;
    private final DeterministicValidationService validationService;
    private final ObjectMapper objectMapper;

    public ValidateInvoiceNodeExecutor(
            InvoiceRepository invoiceRepository,
            DeterministicValidationService validationService,
            ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.VALIDATE_INVOICE;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        try {
            Invoice invoice = invoiceRepository.findById(context.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + context.getInvoiceId()));

            invoice.setProcessingStatus(ProcessingStatus.VALIDATING);
            ValidationResult result = validationService.validateInvoice(invoice);

            invoice.setValidationStatus(result.getOverallStatus());
            invoiceRepository.save(invoice);

            String resultJson = objectMapper.writeValueAsString(result);
            context.getContextData().put("validationResult", result);
            context.getContextData().put("validationStatus", result.getOverallStatus().name());

            return NodeExecutionResult.completed(resultJson);

        } catch (Exception ex) {
            return NodeExecutionResult.failed("Deterministic validation failed: " + ex.getMessage());
        }
    }
}
