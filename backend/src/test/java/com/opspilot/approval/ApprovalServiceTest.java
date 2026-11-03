package com.opspilot.approval;

import com.opspilot.approval.dto.ApprovalDecisionRequest;
import com.opspilot.approval.dto.ApprovalTaskDto;
import com.opspilot.approval.entity.ApprovalDecision;
import com.opspilot.approval.entity.ApprovalTask;
import com.opspilot.approval.entity.DecisionType;
import com.opspilot.approval.repository.ApprovalDecisionRepository;
import com.opspilot.approval.repository.ApprovalTaskRepository;
import com.opspilot.approval.service.ApprovalService;
import com.opspilot.audit.repository.AuditEventRepository;
import com.opspilot.audit.repository.OutboxEventRepository;
import com.opspilot.audit.service.AuditService;
import com.opspilot.common.exception.ApiException;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.workflow.service.WorkflowEngineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ApprovalServiceTest {

    private ApprovalService approvalService;
    private TestWorkflowEngine testWorkflowEngine;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Map<UUID, ApprovalTask> taskStore = new HashMap<>();
    private final Map<UUID, Invoice> invoiceStore = new HashMap<>();
    private final Map<UUID, Supplier> supplierStore = new HashMap<>();

    static class TestWorkflowEngine extends WorkflowEngineService {
        UUID resumedExecutionId;
        UUID cancelledExecutionId;

        public TestWorkflowEngine() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public void resumeWorkflowAfterApproval(UUID workflowExecutionId) {
            this.resumedExecutionId = workflowExecutionId;
        }

        @Override
        public void cancelWorkflowExecution(UUID workflowExecutionId, String reason) {
            this.cancelledExecutionId = workflowExecutionId;
        }
    }

    @BeforeEach
    public void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        taskStore.clear();
        invoiceStore.clear();
        supplierStore.clear();

        testWorkflowEngine = new TestWorkflowEngine();

        // ApprovalTaskRepository proxy
        var taskRepo = (ApprovalTaskRepository) Proxy.newProxyInstance(
                ApprovalTaskRepository.class.getClassLoader(),
                new Class<?>[]{ApprovalTaskRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByIdAndOrganizationId")) {
                        UUID id = (UUID) args[0];
                        return Optional.ofNullable(taskStore.get(id));
                    }
                    if (method.getName().equals("findByOrganizationIdAndStatus")) {
                        return new PageImpl<>(new ArrayList<>(taskStore.values()));
                    }
                    if (method.getName().equals("save")) {
                        ApprovalTask t = (ApprovalTask) args[0];
                        if (t.getId() == null) t.setId(UUID.randomUUID());
                        taskStore.put(t.getId(), t);
                        return t;
                    }
                    return null;
                }
        );

        // ApprovalDecisionRepository proxy
        var decisionRepo = (ApprovalDecisionRepository) Proxy.newProxyInstance(
                ApprovalDecisionRepository.class.getClassLoader(),
                new Class<?>[]{ApprovalDecisionRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        ApprovalDecision d = (ApprovalDecision) args[0];
                        if (d.getId() == null) d.setId(UUID.randomUUID());
                        return d;
                    }
                    return null;
                }
        );

        // InvoiceRepository proxy
        var invoiceRepo = (InvoiceRepository) Proxy.newProxyInstance(
                InvoiceRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        UUID id = (UUID) args[0];
                        return Optional.ofNullable(invoiceStore.get(id));
                    }
                    if (method.getName().equals("save")) {
                        Invoice inv = (Invoice) args[0];
                        invoiceStore.put(inv.getId(), inv);
                        return inv;
                    }
                    return null;
                }
        );

        // UserRepository proxy
        var userRepo = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        User u = new User("approver@opspilot.io", "hash", "Michael", "Scott");
                        u.setId((UUID) args[0]);
                        return Optional.of(u);
                    }
                    return Optional.empty();
                }
        );

        // SupplierRepository proxy
        var supplierRepo = (SupplierRepository) Proxy.newProxyInstance(
                SupplierRepository.class.getClassLoader(),
                new Class<?>[]{SupplierRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByIdAndOrganizationId")) {
                        UUID id = (UUID) args[0];
                        return Optional.ofNullable(supplierStore.get(id));
                    }
                    return Optional.empty();
                }
        );

        // PurchaseOrderRepository proxy
        var poRepo = (PurchaseOrderRepository) Proxy.newProxyInstance(
                PurchaseOrderRepository.class.getClassLoader(),
                new Class<?>[]{PurchaseOrderRepository.class},
                (proxy, method, args) -> Optional.empty()
        );

        // AuditService
        var auditEventRepo = (AuditEventRepository) Proxy.newProxyInstance(
                AuditEventRepository.class.getClassLoader(),
                new Class<?>[]{AuditEventRepository.class},
                (proxy, method, args) -> null
        );
        var outboxRepo = (OutboxEventRepository) Proxy.newProxyInstance(
                OutboxEventRepository.class.getClassLoader(),
                new Class<?>[]{OutboxEventRepository.class},
                (proxy, method, args) -> null
        );
        AuditService auditService = new AuditService(auditEventRepo, outboxRepo);

        approvalService = new ApprovalService(
                taskRepo,
                decisionRepo,
                invoiceRepo,
                userRepo,
                supplierRepo,
                poRepo,
                testWorkflowEngine,
                auditService
        );
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Get inbox returns pending tasks enriched with invoice and supplier data")
    public void testGetInbox() {
        Supplier s = new Supplier(tenantId, "SUP-001", "Dunder Mifflin Paper");
        s.setId(UUID.randomUUID());
        supplierStore.put(s.getId(), s);

        Invoice inv = new Invoice(tenantId, "INV-PAPER-1", UUID.randomUUID());
        inv.setId(UUID.randomUUID());
        inv.setSupplierId(s.getId());
        inv.setTotalAmount(new BigDecimal("6500.00"));
        invoiceStore.put(inv.getId(), inv);

        ApprovalTask task = new ApprovalTask(tenantId, UUID.randomUUID(), null, inv.getId(), null, "APPROVER");
        task.setId(UUID.randomUUID());
        taskStore.put(task.getId(), task);

        PagedResponse<ApprovalTaskDto> inbox = approvalService.getInbox(PageRequest.of(0, 10));

        assertNotNull(inbox);
        assertEquals(1, inbox.getContent().size());
        ApprovalTaskDto item = inbox.getContent().get(0);
        assertEquals("INV-PAPER-1", item.getInvoice().getInvoiceNumber());
        assertEquals("Dunder Mifflin Paper", item.getInvoice().getSupplierName());
        assertEquals(new BigDecimal("6500.00"), item.getInvoice().getTotalAmount());
    }

    @Test
    @DisplayName("Approving task updates invoice to APPROVED and resumes workflow execution")
    public void testApproveDecision() {
        UUID execId = UUID.randomUUID();
        Invoice inv = new Invoice(tenantId, "INV-APPROVE-1", UUID.randomUUID());
        inv.setId(UUID.randomUUID());
        inv.setApprovalStatus(ApprovalStatus.PENDING);
        inv.setProcessingStatus(ProcessingStatus.PENDING_APPROVAL);
        invoiceStore.put(inv.getId(), inv);

        ApprovalTask task = new ApprovalTask(tenantId, execId, null, inv.getId(), null, "APPROVER");
        task.setId(UUID.randomUUID());
        taskStore.put(task.getId(), task);

        ApprovalDecisionRequest req = new ApprovalDecisionRequest();
        req.setDecision(DecisionType.APPROVED);
        req.setComments("Budget approved by regional director.");

        ApprovalDecision decision = approvalService.decide(task.getId(), userId, req);

        assertNotNull(decision);
        assertEquals(DecisionType.APPROVED, decision.getDecision());
        assertEquals(ApprovalTask.Status.DECIDED, task.getStatus());
        assertEquals(ApprovalStatus.APPROVED, inv.getApprovalStatus());
        assertEquals(ProcessingStatus.APPROVED, inv.getProcessingStatus());
        assertEquals(execId, testWorkflowEngine.resumedExecutionId);
    }

    @Test
    @DisplayName("Rejecting task updates invoice to REJECTED and cancels workflow execution")
    public void testRejectDecision() {
        UUID execId = UUID.randomUUID();
        Invoice inv = new Invoice(tenantId, "INV-REJECT-1", UUID.randomUUID());
        inv.setId(UUID.randomUUID());
        inv.setApprovalStatus(ApprovalStatus.PENDING);
        invoiceStore.put(inv.getId(), inv);

        ApprovalTask task = new ApprovalTask(tenantId, execId, null, inv.getId(), null, "APPROVER");
        task.setId(UUID.randomUUID());
        taskStore.put(task.getId(), task);

        ApprovalDecisionRequest req = new ApprovalDecisionRequest();
        req.setDecision(DecisionType.REJECTED);
        req.setComments("Unauthorized price increase.");

        ApprovalDecision decision = approvalService.decide(task.getId(), userId, req);

        assertNotNull(decision);
        assertEquals(DecisionType.REJECTED, decision.getDecision());
        assertEquals(ApprovalStatus.REJECTED, inv.getApprovalStatus());
        assertEquals(ProcessingStatus.REJECTED, inv.getProcessingStatus());
        assertEquals(execId, testWorkflowEngine.cancelledExecutionId);
    }

    @Test
    @DisplayName("Deciding an already decided task throws ApiException with TASK_ALREADY_DECIDED")
    public void testDoubleDecisionThrowsException() {
        Invoice inv = new Invoice(tenantId, "INV-ONCE", UUID.randomUUID());
        inv.setId(UUID.randomUUID());
        invoiceStore.put(inv.getId(), inv);

        ApprovalTask task = new ApprovalTask(tenantId, UUID.randomUUID(), null, inv.getId(), null, "APPROVER");
        task.setId(UUID.randomUUID());
        task.setStatus(ApprovalTask.Status.DECIDED);
        taskStore.put(task.getId(), task);

        ApprovalDecisionRequest req = new ApprovalDecisionRequest();
        req.setDecision(DecisionType.APPROVED);

        ApiException ex = assertThrows(ApiException.class, () ->
                approvalService.decide(task.getId(), userId, req)
        );

        assertEquals("TASK_ALREADY_DECIDED", ex.getCode());
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    @DisplayName("Escalating pending approval task reassigns role to ADMIN and records escalation")
    public void testEscalateApprovalTask() {
        Invoice inv = new Invoice(tenantId, "INV-ESCALATE-1", UUID.randomUUID());
        inv.setId(UUID.randomUUID());
        invoiceStore.put(inv.getId(), inv);

        ApprovalTask task = new ApprovalTask(tenantId, UUID.randomUUID(), null, inv.getId(), null, "APPROVER");
        task.setId(UUID.randomUUID());
        taskStore.put(task.getId(), task);

        ApprovalTaskDto escalated = approvalService.escalate(task.getId(), userId, "Invoice total exceeds department limit");

        assertNotNull(escalated);
        assertEquals("ADMIN", escalated.getAssignedRole());
        assertEquals("ADMIN", task.getAssignedRole());
    }
}
