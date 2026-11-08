package com.opspilot.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.ai.provider.InvoiceExtractionRequest;
import com.opspilot.ai.provider.InvoiceExtractionResult;
import com.opspilot.ai.service.AiExtractionService;
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
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.document.service.DocumentStorageService;
import com.opspilot.document.service.DocumentTextExtractor;
import com.opspilot.export.entity.ExportRecord;
import com.opspilot.export.provider.CsvExportProvider;
import com.opspilot.export.provider.JsonExportProvider;
import com.opspilot.export.provider.MockErpExportProvider;
import com.opspilot.export.repository.ExportRecordRepository;
import com.opspilot.export.service.InvoiceExportService;
import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceLineRepository;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.notification.email.MockEmailProvider;
import com.opspilot.notification.entity.Notification;
import com.opspilot.notification.repository.NotificationRepository;
import com.opspilot.notification.service.NotificationService;
import com.opspilot.purchaseorder.entity.PurchaseOrder;
import com.opspilot.purchaseorder.entity.PurchaseOrderLine;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.supplier.entity.Supplier;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.validation.service.DeterministicValidationService;
import com.opspilot.validation.service.PurchaseOrderMatchingService;
import com.opspilot.workflow.entity.*;
import com.opspilot.workflow.model.NodeType;
import com.opspilot.workflow.model.WorkflowEdge;
import com.opspilot.workflow.model.WorkflowGraph;
import com.opspilot.workflow.model.WorkflowNode;
import com.opspilot.workflow.node.*;
import com.opspilot.workflow.repository.*;
import com.opspilot.workflow.service.ConditionEvaluator;
import com.opspilot.workflow.service.WorkflowEngineService;
import com.opspilot.workflow.service.WorkflowValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EndToEndOperationalPipelineTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID operatorId = UUID.randomUUID();
    private final UUID approverId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    // In-memory data repositories
    private final Map<UUID, Supplier> supplierStore = new HashMap<>();
    private final Map<UUID, PurchaseOrder> poStore = new HashMap<>();
    private final Map<UUID, Invoice> invoiceStore = new HashMap<>();
    private final Map<UUID, InvoiceLine> invoiceLineStore = new HashMap<>();
    private final Map<UUID, WorkflowDefinition> defStore = new HashMap<>();
    private final Map<UUID, WorkflowVersion> versionStore = new HashMap<>();
    private final Map<UUID, WorkflowExecution> execStore = new HashMap<>();
    private final Map<UUID, WorkflowNodeExecution> nodeExecStore = new HashMap<>();
    private final Map<UUID, ApprovalTask> taskStore = new HashMap<>();
    private final Map<UUID, ApprovalDecision> decisionStore = new HashMap<>();
    private final Map<String, ExportRecord> exportStore = new HashMap<>();
    private final Map<UUID, Notification> notificationStore = new HashMap<>();
    private final List<String> auditLog = new ArrayList<>();

    // Services
    private WorkflowEngineService workflowEngineService;
    private ApprovalService approvalService;
    private InvoiceExportService invoiceExportService;
    private NotificationService notificationService;
    private AuditService auditService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        TenantContext.setCurrentTenant(tenantId);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 1. Supplier Repository
        SupplierRepository supplierRepo = (SupplierRepository) Proxy.newProxyInstance(
                SupplierRepository.class.getClassLoader(),
                new Class<?>[]{SupplierRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Supplier s = (Supplier) args[0];
                        if (s.getId() == null) s.setId(UUID.randomUUID());
                        supplierStore.put(s.getId(), s);
                        return s;
                    }
                    if ("findByIdAndOrganizationId".equals(name) || "findById".equals(name)) {
                        return Optional.ofNullable(supplierStore.get(args[0]));
                    }
                    if ("findByOrganizationIdAndNameIgnoreCase".equals(name)) {
                        String supName = (String) args[1];
                        return supplierStore.values().stream()
                                .filter(s -> supName.equalsIgnoreCase(s.getName()))
                                .findFirst();
                    }
                    return null;
                }
        );

        // 2. Purchase Order Repository
        PurchaseOrderRepository poRepo = (PurchaseOrderRepository) Proxy.newProxyInstance(
                PurchaseOrderRepository.class.getClassLoader(),
                new Class<?>[]{PurchaseOrderRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        PurchaseOrder po = (PurchaseOrder) args[0];
                        if (po.getId() == null) po.setId(UUID.randomUUID());
                        poStore.put(po.getId(), po);
                        return po;
                    }
                    if ("findByIdAndOrganizationId".equals(name) || "findById".equals(name)) {
                        return Optional.ofNullable(poStore.get(args[0]));
                    }
                    return null;
                }
        );

        // 3. Invoice Repositories
        InvoiceRepository invoiceRepo = (InvoiceRepository) Proxy.newProxyInstance(
                InvoiceRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Invoice inv = (Invoice) args[0];
                        if (inv.getId() == null) inv.setId(UUID.randomUUID());
                        invoiceStore.put(inv.getId(), inv);
                        return inv;
                    }
                    if ("findById".equals(name) || "findByIdAndOrganizationId".equals(name)) {
                        return Optional.ofNullable(invoiceStore.get(args[0]));
                    }
                    if ("findPotentialDuplicates".equals(name)) {
                        return List.of();
                    }
                    return null;
                }
        );

        InvoiceLineRepository invoiceLineRepo = (InvoiceLineRepository) Proxy.newProxyInstance(
                InvoiceLineRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceLineRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        InvoiceLine l = (InvoiceLine) args[0];
                        if (l.getId() == null) l.setId(UUID.randomUUID());
                        invoiceLineStore.put(l.getId(), l);
                        return l;
                    }
                    if ("findByInvoiceId".equals(name)) {
                        return invoiceLineStore.values().stream().toList();
                    }
                    if ("deleteAll".equals(name)) {
                        invoiceLineStore.clear();
                        return null;
                    }
                    return null;
                }
        );

        // 4. Audit Service
        AuditEventRepository aer = (AuditEventRepository) Proxy.newProxyInstance(
                AuditEventRepository.class.getClassLoader(), new Class<?>[]{AuditEventRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) auditLog.add(a[0].toString());
                    return null;
                });
        OutboxEventRepository oer = (OutboxEventRepository) Proxy.newProxyInstance(
                OutboxEventRepository.class.getClassLoader(), new Class<?>[]{OutboxEventRepository.class}, (p, m, a) -> null);
        auditService = new AuditService(aer, oer);

        // 5. User Repository
        UserRepository userRepo = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (p, m, a) -> {
                    UUID id = (UUID) a[0];
                    User u = new User("admin@opspilot.io", "hash", "Admin", "User");
                    u.setId(id);
                    return Optional.of(u);
                }
        );

        // 6. Approval Repositories
        ApprovalTaskRepository taskRepo = (ApprovalTaskRepository) Proxy.newProxyInstance(
                ApprovalTaskRepository.class.getClassLoader(),
                new Class<?>[]{ApprovalTaskRepository.class},
                (p, m, a) -> {
                    String name = m.getName();
                    if ("save".equals(name)) {
                        ApprovalTask t = (ApprovalTask) a[0];
                        if (t.getId() == null) t.setId(UUID.randomUUID());
                        taskStore.put(t.getId(), t);
                        return t;
                    }
                    if ("findByIdAndOrganizationId".equals(name) || "findById".equals(name)) {
                        return Optional.ofNullable(taskStore.get(a[0]));
                    }
                    return null;
                }
        );

        ApprovalDecisionRepository decisionRepo = (ApprovalDecisionRepository) Proxy.newProxyInstance(
                ApprovalDecisionRepository.class.getClassLoader(),
                new Class<?>[]{ApprovalDecisionRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) {
                        ApprovalDecision d = (ApprovalDecision) a[0];
                        if (d.getId() == null) d.setId(UUID.randomUUID());
                        decisionStore.put(d.getId(), d);
                        return d;
                    }
                    return null;
                }
        );

        // 7. Notification Service
        NotificationRepository notifRepo = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) {
                        Notification n = (Notification) a[0];
                        if (n.getId() == null) n.setId(UUID.randomUUID());
                        notificationStore.put(n.getId(), n);
                        return n;
                    }
                    return null;
                }
        );
        MockEmailProvider emailProvider = new MockEmailProvider();
        notificationService = new NotificationService(notifRepo, emailProvider);

        // 8. Export Service
        ExportRecordRepository exportRepo = (ExportRecordRepository) Proxy.newProxyInstance(
                ExportRecordRepository.class.getClassLoader(),
                new Class<?>[]{ExportRecordRepository.class},
                (p, m, a) -> {
                    String name = m.getName();
                    if ("save".equals(name)) {
                        ExportRecord r = (ExportRecord) a[0];
                        if (r.getId() == null) r.setId(UUID.randomUUID());
                        exportStore.put(r.getIdempotencyKey(), r);
                        return r;
                    }
                    if ("findByIdempotencyKey".equals(name)) {
                        return Optional.ofNullable(exportStore.get(a[0]));
                    }
                    return null;
                }
        );
        invoiceExportService = new InvoiceExportService(
                invoiceRepo,
                invoiceLineRepo,
                exportRepo,
                auditService,
                List.of(
                        new CsvExportProvider(),
                        new JsonExportProvider(objectMapper),
                        new MockErpExportProvider()
                )
        );

        // 9. Workflow Repositories
        WorkflowDefinitionRepository defRepo = (WorkflowDefinitionRepository) Proxy.newProxyInstance(
                WorkflowDefinitionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowDefinitionRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) {
                        WorkflowDefinition d = (WorkflowDefinition) a[0];
                        if (d.getId() == null) d.setId(UUID.randomUUID());
                        defStore.put(d.getId(), d);
                        return d;
                    }
                    if ("findById".equals(m.getName()) || "findByIdAndOrganizationId".equals(m.getName())) {
                        return Optional.ofNullable(defStore.get(a[0]));
                    }
                    return null;
                }
        );

        WorkflowVersionRepository versionRepo = (WorkflowVersionRepository) Proxy.newProxyInstance(
                WorkflowVersionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowVersionRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) {
                        WorkflowVersion v = (WorkflowVersion) a[0];
                        if (v.getId() == null) v.setId(UUID.randomUUID());
                        versionStore.put(v.getId(), v);
                        return v;
                    }
                    if ("findById".equals(m.getName())) {
                        return Optional.ofNullable(versionStore.get(a[0]));
                    }
                    if ("findByWorkflowDefinitionIdOrderByVersionNumberDesc".equals(m.getName())) {
                        return versionStore.values().stream().toList();
                    }
                    return null;
                }
        );

        WorkflowExecutionRepository execRepo = (WorkflowExecutionRepository) Proxy.newProxyInstance(
                WorkflowExecutionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowExecutionRepository.class},
                (p, m, a) -> {
                    if ("save".equals(m.getName())) {
                        WorkflowExecution e = (WorkflowExecution) a[0];
                        if (e.getId() == null) e.setId(UUID.randomUUID());
                        execStore.put(e.getId(), e);
                        return e;
                    }
                    if ("findById".equals(m.getName()) || "findByIdAndOrganizationId".equals(m.getName())) {
                        return Optional.ofNullable(execStore.get(a[0]));
                    }
                    return null;
                }
        );

        WorkflowNodeExecutionRepository nodeExecRepo = (WorkflowNodeExecutionRepository) Proxy.newProxyInstance(
                WorkflowNodeExecutionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowNodeExecutionRepository.class},
                (p, m, a) -> {
                    String name = m.getName();
                    if ("save".equals(name)) {
                        WorkflowNodeExecution ne = (WorkflowNodeExecution) a[0];
                        if (ne.getId() == null) ne.setId(UUID.randomUUID());
                        nodeExecStore.put(ne.getId(), ne);
                        return ne;
                    }
                    if ("findByIdempotencyKey".equals(name)) {
                        String key = (String) a[0];
                        return nodeExecStore.values().stream().filter(n -> key.equals(n.getIdempotencyKey())).findFirst();
                    }
                    if ("findByWorkflowExecutionIdOrderByCreatedAtAsc".equals(name)) {
                        UUID eid = (UUID) a[0];
                        return nodeExecStore.values().stream().filter(n -> eid.equals(n.getWorkflowExecutionId())).toList();
                    }
                    return null;
                }
        );

        WorkflowJobRepository jobRepo = (WorkflowJobRepository) Proxy.newProxyInstance(
                WorkflowJobRepository.class.getClassLoader(), new Class<?>[]{WorkflowJobRepository.class}, (p, m, a) -> null);

        // 10. AI Extraction Provider Mock
        com.opspilot.ai.repository.AiInvocationRepository aiInvocationRepo = (com.opspilot.ai.repository.AiInvocationRepository) Proxy.newProxyInstance(
                com.opspilot.ai.repository.AiInvocationRepository.class.getClassLoader(),
                new Class<?>[]{com.opspilot.ai.repository.AiInvocationRepository.class},
                (p, m, a) -> null
        );

        com.opspilot.ai.mock.MockAiProvider mockAiProvider = new com.opspilot.ai.mock.MockAiProvider() {
            @Override
            public InvoiceExtractionResult extractInvoice(InvoiceExtractionRequest request) {
                InvoiceExtractionResult res = new InvoiceExtractionResult();
                res.setInvoiceNumber("INV-ACME-900");
                res.setInvoiceDate(LocalDate.now().minusDays(2));
                res.setDueDate(LocalDate.now().plusDays(28));
                res.setSupplierName("Acme Industrial Tools Inc.");
                res.setCurrency("USD");
                res.setSubtotal(new BigDecimal("7500.00"));
                res.setTaxAmount(new BigDecimal("750.00"));
                res.setTotalAmount(new BigDecimal("8250.00"));
                res.setSuccess(true);
                res.setLineItems(List.of(
                        new InvoiceExtractionResult.ExtractedLineItem(
                                "Heavy Drill Set", new BigDecimal("5.0000"), new BigDecimal("1000.00"), new BigDecimal("10.00"), new BigDecimal("5000.00")),
                        new InvoiceExtractionResult.ExtractedLineItem(
                                "Pneumatic Tool Kit", new BigDecimal("2.5000"), new BigDecimal("1000.00"), new BigDecimal("10.00"), new BigDecimal("2500.00"))
                ));
                return res;
            }
        };
        AiExtractionService aiService = new AiExtractionService(mockAiProvider, null, aiInvocationRepo, "mock");

        // 11. Domain Executors
        DeterministicValidationService validationService = new DeterministicValidationService(invoiceRepo, supplierRepo);
        PurchaseOrderMatchingService poMatchingService = new PurchaseOrderMatchingService(poRepo);

        List<WorkflowNodeExecutor> executors = List.of(
                new StartNodeExecutor(),
                new AiExtractionNodeExecutor(invoiceRepo, invoiceLineRepo, supplierRepo, null, null, aiService, objectMapper),
                new ValidateInvoiceNodeExecutor(invoiceRepo, validationService, objectMapper),
                new MatchPurchaseOrderNodeExecutor(invoiceRepo, poMatchingService, objectMapper),
                new ConditionNodeExecutor(new ConditionEvaluator()),
                new ApprovalNodeExecutor(taskRepo, invoiceRepo),
                new NotificationNodeExecutor(notificationService),
                new ExportInvoiceNodeExecutor(invoiceExportService, invoiceRepo),
                new EndNodeExecutor()
        );

        NodeExecutorRegistry registry = new NodeExecutorRegistry(executors);

        workflowEngineService = new WorkflowEngineService(
                defRepo,
                versionRepo,
                execRepo,
                nodeExecRepo,
                jobRepo,
                invoiceRepo,
                registry,
                new WorkflowValidationService(),
                auditService,
                objectMapper
        );

        approvalService = new ApprovalService(
                taskRepo,
                decisionRepo,
                invoiceRepo,
                userRepo,
                supplierRepo,
                poRepo,
                workflowEngineService,
                auditService,
                notificationService
        );
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("End-to-End AP Lifecycle: Intake -> AI Extract -> Validate -> Match -> Approval Pause -> Escalate -> Decide -> Auto-Resume -> Export")
    public void testCompleteAccountsPayableLifecycle() throws Exception {
        // --- 1. Register Supplier ---
        Supplier supplier = new Supplier(tenantId, "SUP-ACME", "Acme Industrial Tools Inc.");
        supplier.setId(UUID.randomUUID());
        supplierStore.put(supplier.getId(), supplier);

        // --- 2. Register Purchase Order ---
        PurchaseOrder po = new PurchaseOrder(tenantId, supplier.getId(), "PO-2026-900", LocalDate.now().minusDays(5), "USD");
        po.setId(UUID.randomUUID());
        po.setSubtotal(new BigDecimal("7500.00"));
        po.setTaxAmount(new BigDecimal("750.00"));
        po.setTotalAmount(new BigDecimal("8250.00"));
        poStore.put(po.getId(), po);

        // --- 3. Build Full Accounts Payable Workflow DAG ---
        List<WorkflowNode> nodes = List.of(
                new WorkflowNode("start-1", NodeType.START, "Start Intake", null),
                new WorkflowNode("extract-1", NodeType.AI_EXTRACTION, "AI Extraction", null),
                new WorkflowNode("validate-1", NodeType.VALIDATE_INVOICE, "Deterministic Validation", null),
                new WorkflowNode("match-po-1", NodeType.MATCH_PURCHASE_ORDER, "PO 2-Way Match", null),
                new WorkflowNode("condition-1", NodeType.CONDITION, "Validation Passed?", null),
                new WorkflowNode("approval-1", NodeType.APPROVAL, "Human Approval Task", null),
                new WorkflowNode("notify-1", NodeType.NOTIFICATION, "Dispatch Alert", null),
                new WorkflowNode("export-1", NodeType.EXPORT_INVOICE, "Export to ERP", null),
                new WorkflowNode("end-1", NodeType.END, "End Pipeline", null)
        );

        List<WorkflowEdge> edges = List.of(
                new WorkflowEdge("start-1", "extract-1"),
                new WorkflowEdge("extract-1", "validate-1"),
                new WorkflowEdge("validate-1", "match-po-1"),
                new WorkflowEdge("match-po-1", "condition-1"),
                new WorkflowEdge("condition-1", "approval-1", "TRUE"),
                new WorkflowEdge("approval-1", "notify-1"),
                new WorkflowEdge("notify-1", "export-1"),
                new WorkflowEdge("export-1", "end-1")
        );

        WorkflowGraph graph = new WorkflowGraph(nodes, edges);
        String graphJson = objectMapper.writeValueAsString(graph);

        WorkflowDefinition def = new WorkflowDefinition(tenantId, "Full AP Pipeline", "End to end test", null);
        def.setId(UUID.randomUUID());
        defStore.put(def.getId(), def);

        WorkflowVersion version = new WorkflowVersion(def.getId(), 1, graphJson, null);
        version.setId(UUID.randomUUID());
        versionStore.put(version.getId(), version);

        // --- 4. Intake Invoice Document ---
        Invoice invoice = new Invoice(tenantId, "INV-RAW-TEMP", null);
        invoice.setId(UUID.randomUUID());
        invoice.setPurchaseOrderId(po.getId());
        invoiceStore.put(invoice.getId(), invoice);

        // --- 5. Launch Workflow Pipeline ---
        WorkflowExecution execution = workflowEngineService.startWorkflow(def.getId(), invoice.getId());
        assertNotNull(execution);

        if (execution.getStatus() == WorkflowExecution.Status.FAILED) {
            System.err.println("DEBUG FAILED EXECUTION: " + execution.getErrorMessage());
            for (WorkflowNodeExecution ne : nodeExecStore.values()) {
                System.err.println("  Node: " + ne.getNodeId() + " [" + ne.getNodeType() + "] status=" + ne.getStatus() + " error=" + ne.getErrorDetails());
            }
        }

        // Pipeline executes start -> extract -> validate -> match-po -> condition -> approval
        // Because approval node pauses execution, the workflow should enter WAITING_APPROVAL!
        assertEquals(WorkflowExecution.Status.WAITING_APPROVAL, execution.getStatus(),
                "Workflow must safely pause at the human approval node");

        // Verify invoice was populated by AI extraction
        Invoice extractedInvoice = invoiceStore.get(invoice.getId());
        assertEquals("INV-ACME-900", extractedInvoice.getInvoiceNumber());
        assertEquals(new BigDecimal("8250.00"), extractedInvoice.getTotalAmount());
        assertEquals(supplier.getId(), extractedInvoice.getSupplierId(), "Supplier must be auto-resolved by name");
        assertEquals(ProcessingStatus.PENDING_APPROVAL, extractedInvoice.getProcessingStatus());

        // --- 6. Verify Human Approval Task in Inbox ---
        assertEquals(1, taskStore.size(), "Approval task must be generated");
        ApprovalTask task = taskStore.values().iterator().next();
        assertEquals(ApprovalTask.Status.PENDING, task.getStatus());
        assertEquals("APPROVER", task.getAssignedRole());

        // --- 7. Test Approval Task Escalation ---
        ApprovalTaskDto escalated = approvalService.escalate(task.getId(), operatorId, "Total exceeds $5000 standard tier");
        assertNotNull(escalated);
        assertEquals("ADMIN", escalated.getAssignedRole());
        assertEquals(1, notificationStore.size(), "In-app notification must be delivered on escalation");

        // --- 8. Admin Decides: APPROVED ---
        ApprovalDecisionRequest decisionRequest = new ApprovalDecisionRequest();
        decisionRequest.setDecision(DecisionType.APPROVED);
        decisionRequest.setComments("Verified PO matching and budget sign-off granted");

        ApprovalDecision decision = approvalService.decide(task.getId(), adminId, decisionRequest);
        assertNotNull(decision);
        assertEquals(DecisionType.APPROVED, decision.getDecision());
        assertEquals(ApprovalTask.Status.DECIDED, task.getStatus());

        // --- 9. Verify Post-Approval Auto-Resumption & Completion ---
        // Resuming advances: approval -> notify -> export -> end!
        WorkflowExecution completedExec = execStore.get(execution.getId());
        assertEquals(WorkflowExecution.Status.COMPLETED, completedExec.getStatus(),
                "Workflow execution must advance through export and complete");

        // --- 10. Verify Accounting Export Record ---
        assertEquals(1, exportStore.size(), "Export record must be generated");
        ExportRecord exportRecord = exportStore.values().iterator().next();
        assertEquals("COMPLETED", exportRecord.getStatus());
        assertEquals("CSV", exportRecord.getExportFormat());
        assertNotNull(exportRecord.getIdempotencyKey());

        // --- 11. Verify Final Invoice State ---
        Invoice finalInvoice = invoiceStore.get(invoice.getId());
        assertEquals(ApprovalStatus.APPROVED, finalInvoice.getApprovalStatus());
        assertEquals(ProcessingStatus.EXPORTED, finalInvoice.getProcessingStatus());

        // --- 12. Verify Notifications ---
        assertTrue(notificationStore.size() >= 2, "Notifications for escalation and workflow alert must be present");
    }
}
