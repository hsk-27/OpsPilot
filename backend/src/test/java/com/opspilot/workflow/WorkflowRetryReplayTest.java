package com.opspilot.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.audit.repository.AuditEventRepository;
import com.opspilot.audit.repository.OutboxEventRepository;
import com.opspilot.audit.service.AuditService;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.entity.*;
import com.opspilot.workflow.model.NodeType;
import com.opspilot.workflow.model.WorkflowGraph;
import com.opspilot.workflow.model.WorkflowNode;
import com.opspilot.workflow.node.NodeExecutionResult;
import com.opspilot.workflow.node.NodeExecutorRegistry;
import com.opspilot.workflow.node.WorkflowNodeExecutor;
import com.opspilot.workflow.repository.*;
import com.opspilot.workflow.service.WorkflowEngineService;
import com.opspilot.workflow.service.WorkflowValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowRetryReplayTest {

    private WorkflowEngineService workflowEngineService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID defId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();

    private final Map<UUID, WorkflowExecution> executionStore = new HashMap<>();
    private final Map<UUID, WorkflowNodeExecution> nodeExecutionStore = new HashMap<>();
    private final Map<UUID, WorkflowDefinition> defStore = new HashMap<>();
    private final Map<UUID, WorkflowVersion> versionStore = new HashMap<>();
    private final Map<UUID, Invoice> invoiceStore = new HashMap<>();

    private int executionAttemptCount = 0;

    @BeforeEach
    public void setUp() throws Exception {
        TenantContext.setCurrentTenant(tenantId);
        executionStore.clear();
        nodeExecutionStore.clear();
        defStore.clear();
        versionStore.clear();
        invoiceStore.clear();
        executionAttemptCount = 0;

        ObjectMapper mapper = new ObjectMapper();

        Invoice invoice = new Invoice(tenantId, "INV-RETRY-01", UUID.randomUUID());
        invoice.setId(invoiceId);
        invoiceStore.put(invoiceId, invoice);

        WorkflowDefinition def = new WorkflowDefinition(tenantId, "AP Retry Pipeline", "Testing retry", null);
        def.setId(defId);
        defStore.put(defId, def);

        List<WorkflowNode> nodes = List.of(
                new WorkflowNode("start-1", NodeType.START, "Start", null),
                new WorkflowNode("end-1", NodeType.END, "End", null)
        );
        WorkflowGraph graph = new WorkflowGraph(nodes, List.of(new com.opspilot.workflow.model.WorkflowEdge("start-1", "end-1")));
        String graphJson = mapper.writeValueAsString(graph);

        WorkflowVersion version = new WorkflowVersion(defId, 1, graphJson, null);
        version.setId(versionId);
        versionStore.put(versionId, version);

        // Proxies
        WorkflowDefinitionRepository defRepo = (WorkflowDefinitionRepository) Proxy.newProxyInstance(
                WorkflowDefinitionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowDefinitionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByIdAndOrganizationId".equals(name)) {
                        return Optional.ofNullable(defStore.get(args[0]));
                    }
                    if ("findByOrganizationIdAndStatus".equals(name)) {
                        return defStore.values().stream().filter(d -> tenantId.equals(d.getOrganizationId())).findFirst();
                    }
                    if ("save".equals(name)) {
                        WorkflowDefinition d = (WorkflowDefinition) args[0];
                        if (d.getId() == null) d.setId(UUID.randomUUID());
                        defStore.put(d.getId(), d);
                        return d;
                    }
                    return null;
                }
        );

        WorkflowVersionRepository versionRepo = (WorkflowVersionRepository) Proxy.newProxyInstance(
                WorkflowVersionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowVersionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByWorkflowDefinitionIdOrderByVersionNumberDesc".equals(name)) {
                        return versionStore.values().stream()
                                .filter(v -> defId.equals(v.getWorkflowDefinitionId()))
                                .sorted(Comparator.comparing(WorkflowVersion::getVersionNumber).reversed())
                                .toList();
                    }
                    if ("findById".equals(name)) {
                        return Optional.ofNullable(versionStore.get(args[0]));
                    }
                    if ("save".equals(name)) {
                        WorkflowVersion v = (WorkflowVersion) args[0];
                        if (v.getId() == null) v.setId(UUID.randomUUID());
                        versionStore.put(v.getId(), v);
                        return v;
                    }
                    return null;
                }
        );

        WorkflowExecutionRepository execRepo = (WorkflowExecutionRepository) Proxy.newProxyInstance(
                WorkflowExecutionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowExecutionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name) || "findByIdAndOrganizationId".equals(name)) {
                        return Optional.ofNullable(executionStore.get(args[0]));
                    }
                    if ("save".equals(name)) {
                        WorkflowExecution e = (WorkflowExecution) args[0];
                        if (e.getId() == null) e.setId(UUID.randomUUID());
                        executionStore.put(e.getId(), e);
                        return e;
                    }
                    return null;
                }
        );

        WorkflowNodeExecutionRepository nodeExecRepo = (WorkflowNodeExecutionRepository) Proxy.newProxyInstance(
                WorkflowNodeExecutionRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowNodeExecutionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByIdempotencyKey".equals(name)) {
                        String key = (String) args[0];
                        return nodeExecutionStore.values().stream()
                                .filter(ne -> key.equals(ne.getIdempotencyKey()))
                                .findFirst();
                    }
                    if ("findByWorkflowExecutionIdOrderByCreatedAtAsc".equals(name)) {
                        UUID execId = (UUID) args[0];
                        return nodeExecutionStore.values().stream()
                                .filter(ne -> execId.equals(ne.getWorkflowExecutionId()))
                                .toList();
                    }
                    if ("save".equals(name)) {
                        WorkflowNodeExecution ne = (WorkflowNodeExecution) args[0];
                        if (ne.getId() == null) ne.setId(UUID.randomUUID());
                        nodeExecutionStore.put(ne.getId(), ne);
                        return ne;
                    }
                    return null;
                }
        );

        WorkflowJobRepository jobRepo = (WorkflowJobRepository) Proxy.newProxyInstance(
                WorkflowJobRepository.class.getClassLoader(),
                new Class<?>[]{WorkflowJobRepository.class},
                (proxy, method, args) -> null
        );

        InvoiceRepository invRepo = (InvoiceRepository) Proxy.newProxyInstance(
                InvoiceRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name) || "findByIdAndOrganizationId".equals(name)) {
                        return Optional.ofNullable(invoiceStore.get(args[0]));
                    }
                    if ("save".equals(name)) {
                        Invoice inv = (Invoice) args[0];
                        invoiceStore.put(inv.getId(), inv);
                        return inv;
                    }
                    return null;
                }
        );

        // Executors
        WorkflowNodeExecutor startExecutor = new WorkflowNodeExecutor() {
            @Override
            public NodeType supportedType() {
                return NodeType.START;
            }

            @Override
            public NodeExecutionResult execute(com.opspilot.workflow.node.NodeExecutionContext context, com.fasterxml.jackson.databind.JsonNode configuration) {
                executionAttemptCount++;
                return NodeExecutionResult.completed("{\"start\":true}");
            }
        };

        WorkflowNodeExecutor endExecutor = new WorkflowNodeExecutor() {
            @Override
            public NodeType supportedType() {
                return NodeType.END;
            }

            @Override
            public NodeExecutionResult execute(com.opspilot.workflow.node.NodeExecutionContext context, com.fasterxml.jackson.databind.JsonNode configuration) {
                return NodeExecutionResult.completed("{\"end\":true}");
            }
        };

        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(startExecutor, endExecutor));

        AuditEventRepository aer = (AuditEventRepository) Proxy.newProxyInstance(
                AuditEventRepository.class.getClassLoader(), new Class<?>[]{AuditEventRepository.class}, (p, m, a) -> null);
        OutboxEventRepository oer = (OutboxEventRepository) Proxy.newProxyInstance(
                OutboxEventRepository.class.getClassLoader(), new Class<?>[]{OutboxEventRepository.class}, (p, m, a) -> null);
        AuditService auditService = new AuditService(aer, oer);

        WorkflowValidationService validationService = new WorkflowValidationService();

        workflowEngineService = new WorkflowEngineService(
                defRepo,
                versionRepo,
                execRepo,
                nodeExecRepo,
                jobRepo,
                invRepo,
                registry,
                validationService,
                auditService,
                mapper
        );
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Trigger workflow starts execution and advances to completed state")
    public void testStartWorkflow() {
        WorkflowExecution exec = workflowEngineService.startWorkflow(defId, invoiceId);
        assertNotNull(exec);
        assertEquals(WorkflowExecution.Status.COMPLETED, exec.getStatus());
        assertEquals(1, executionAttemptCount);
    }

    @Test
    @DisplayName("Retry failed execution resumes execution cleanly")
    public void testRetryExecution() {
        WorkflowExecution exec = new WorkflowExecution(tenantId, defId, versionId, "INVOICE", invoiceId);
        exec.setId(UUID.randomUUID());
        exec.setStatus(WorkflowExecution.Status.FAILED);
        exec.setErrorCode("TEMP_FAILURE");
        exec.setErrorMessage("External endpoint timeout");
        executionStore.put(exec.getId(), exec);

        WorkflowExecution retried = workflowEngineService.retryExecution(exec.getId());
        assertNotNull(retried);
        assertEquals(WorkflowExecution.Status.COMPLETED, retried.getStatus());
        assertNull(retried.getErrorCode());
        assertNull(retried.getErrorMessage());
    }

    @Test
    @DisplayName("Replay execution launches a fresh pipeline execution")
    public void testReplayExecution() {
        WorkflowExecution initial = workflowEngineService.startWorkflow(defId, invoiceId);
        assertNotNull(initial);

        WorkflowExecution replay = workflowEngineService.replayExecution(initial.getId());
        assertNotNull(replay);
        assertNotEquals(initial.getId(), replay.getId());
        assertEquals(WorkflowExecution.Status.COMPLETED, replay.getStatus());
    }
}
