package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.approval.entity.ApprovalTask;
import com.opspilot.approval.repository.ApprovalTaskRepository;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ApprovalNodeExecutor implements WorkflowNodeExecutor {

    private final ApprovalTaskRepository approvalTaskRepository;
    private final InvoiceRepository invoiceRepository;

    public ApprovalNodeExecutor(ApprovalTaskRepository approvalTaskRepository, InvoiceRepository invoiceRepository) {
        this.approvalTaskRepository = approvalTaskRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.APPROVAL;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        Invoice invoice = invoiceRepository.findById(context.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + context.getInvoiceId()));

        String role = configuration != null && configuration.has("approverRole")
                ? configuration.get("approverRole").asText("APPROVER")
                : "APPROVER";

        UUID assignedUser = null;
        if (configuration != null && configuration.has("assignedUserId")) {
            try {
                assignedUser = UUID.fromString(configuration.get("assignedUserId").asText());
            } catch (Exception ignored) {}
        }

        // Create approval task
        ApprovalTask task = new ApprovalTask(
                context.getOrganizationId(),
                context.getWorkflowExecutionId(),
                null,
                invoice.getId(),
                assignedUser,
                role
        );
        task.setDecisionDeadline(Instant.now().plusSeconds(7 * 86400)); // 7 days default
        task = approvalTaskRepository.save(task);

        invoice.setApprovalStatus(ApprovalStatus.PENDING);
        invoice.setProcessingStatus(ProcessingStatus.PENDING_APPROVAL);
        invoiceRepository.save(invoice);

        String snapshot = String.format("{\"approvalTaskId\":\"%s\",\"assignedRole\":\"%s\",\"status\":\"WAITING_APPROVAL\"}",
                task.getId(), role);

        return NodeExecutionResult.waitingApproval(snapshot);
    }
}
