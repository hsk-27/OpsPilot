package com.opspilot.approval.service;

import com.opspilot.approval.dto.ApprovalDecisionRequest;
import com.opspilot.approval.dto.ApprovalTaskDto;
import com.opspilot.approval.entity.ApprovalDecision;
import com.opspilot.approval.entity.ApprovalTask;
import com.opspilot.approval.entity.DecisionType;
import com.opspilot.approval.repository.ApprovalDecisionRepository;
import com.opspilot.approval.repository.ApprovalTaskRepository;
import com.opspilot.audit.service.AuditService;
import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.invoice.dto.InvoiceDto;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.service.WorkflowEngineService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ApprovalService {

    private final ApprovalTaskRepository taskRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final com.opspilot.supplier.repository.SupplierRepository supplierRepository;
    private final com.opspilot.purchaseorder.repository.PurchaseOrderRepository purchaseOrderRepository;
    private final WorkflowEngineService workflowEngineService;
    private final AuditService auditService;

    public ApprovalService(
            ApprovalTaskRepository taskRepository,
            ApprovalDecisionRepository decisionRepository,
            InvoiceRepository invoiceRepository,
            UserRepository userRepository,
            com.opspilot.supplier.repository.SupplierRepository supplierRepository,
            com.opspilot.purchaseorder.repository.PurchaseOrderRepository purchaseOrderRepository,
            @Lazy WorkflowEngineService workflowEngineService,
            AuditService auditService) {
        this.taskRepository = taskRepository;
        this.decisionRepository = decisionRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.workflowEngineService = workflowEngineService;
        this.auditService = auditService;
    }

    private InvoiceDto enrichInvoiceDto(Invoice inv, UUID orgId) {
        if (inv == null) return null;
        InvoiceDto dto = InvoiceDto.from(inv);
        if (inv.getSupplierId() != null) {
            supplierRepository.findByIdAndOrganizationId(inv.getSupplierId(), orgId)
                    .ifPresent(s -> dto.setSupplierName(s.getName()));
        }
        if (inv.getPurchaseOrderId() != null) {
            purchaseOrderRepository.findByIdAndOrganizationId(inv.getPurchaseOrderId(), orgId)
                    .ifPresent(po -> dto.setPurchaseOrderNumber(po.getPurchaseOrderNumber()));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApprovalTaskDto> getInbox(Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        Page<ApprovalTask> page = taskRepository.findByOrganizationIdAndStatus(orgId, ApprovalTask.Status.PENDING, pageable);
        return PagedResponse.from(page.map(task -> {
            Invoice inv = invoiceRepository.findById(task.getInvoiceId()).orElse(null);
            return ApprovalTaskDto.from(task, enrichInvoiceDto(inv, orgId));
        }));
    }

    @Transactional(readOnly = true)
    public ApprovalTaskDto getTask(UUID taskId) {
        UUID orgId = TenantContext.getCurrentTenant();
        ApprovalTask task = taskRepository.findByIdAndOrganizationId(taskId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalTask", taskId));

        Invoice inv = invoiceRepository.findById(task.getInvoiceId()).orElse(null);
        return ApprovalTaskDto.from(task, enrichInvoiceDto(inv, orgId));
    }

    @Transactional
    public synchronized ApprovalDecision decide(UUID taskId, UUID actorUserId, ApprovalDecisionRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        ApprovalTask task = taskRepository.findByIdAndOrganizationId(taskId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalTask", taskId));

        if (task.getStatus() != ApprovalTask.Status.PENDING) {
            throw new ApiException("Approval task is no longer pending (current: " + task.getStatus() + ")",
                    HttpStatus.CONFLICT, "TASK_ALREADY_DECIDED");
        }

        Invoice invoice = invoiceRepository.findById(task.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", task.getInvoiceId()));

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));

        // Persist decision
        ApprovalDecision decision = new ApprovalDecision(
                task.getId(),
                actor.getId(),
                request.getDecision(),
                request.getComments(),
                "{\"approvalStatus\":\"" + invoice.getApprovalStatus() + "\"}",
                "{\"approvalStatus\":\"" + request.getDecision() + "\"}"
        );
        decision = decisionRepository.save(decision);

        // Mark task decided
        task.setStatus(ApprovalTask.Status.DECIDED);
        taskRepository.save(task);

        // Update invoice state
        if (request.getDecision() == DecisionType.APPROVED) {
            invoice.setApprovalStatus(ApprovalStatus.APPROVED);
            invoice.setProcessingStatus(ProcessingStatus.APPROVED);
        } else if (request.getDecision() == DecisionType.REJECTED) {
            invoice.setApprovalStatus(ApprovalStatus.REJECTED);
            invoice.setProcessingStatus(ProcessingStatus.REJECTED);
        } else if (request.getDecision() == DecisionType.CHANGES_REQUESTED) {
            invoice.setApprovalStatus(ApprovalStatus.CHANGES_REQUESTED);
            invoice.setProcessingStatus(ProcessingStatus.NEEDS_REVIEW);
        }
        invoiceRepository.save(invoice);

        // Record audit log
        auditService.recordEvent(
                actor.getId(),
                "USER",
                "APPROVAL_DECISION_" + request.getDecision().name(),
                "INVOICE",
                invoice.getId().toString(),
                "PENDING",
                request.getDecision().name()
        );

        // If APPROVED, resume workflow execution to continue to EXPORT!
        if (request.getDecision() == DecisionType.APPROVED && task.getWorkflowExecutionId() != null) {
            workflowEngineService.resumeWorkflowAfterApproval(task.getWorkflowExecutionId());
        } else if (request.getDecision() == DecisionType.REJECTED && task.getWorkflowExecutionId() != null) {
            workflowEngineService.cancelWorkflowExecution(task.getWorkflowExecutionId(), "REJECTED_BY_APPROVER: " + request.getComments());
        }

        return decision;
    }
}
