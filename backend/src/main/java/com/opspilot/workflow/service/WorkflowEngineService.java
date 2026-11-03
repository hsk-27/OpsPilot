package com.opspilot.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.audit.service.AuditService;
import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.workflow.entity.*;
import com.opspilot.workflow.model.*;
import com.opspilot.workflow.node.NodeExecutionContext;
import com.opspilot.workflow.node.NodeExecutionResult;
import com.opspilot.workflow.node.NodeExecutorRegistry;
import com.opspilot.workflow.node.WorkflowNodeExecutor;
import com.opspilot.workflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowNodeExecutionRepository nodeExecutionRepository;
    private final WorkflowJobRepository jobRepository;
    private final InvoiceRepository invoiceRepository;
    private final NodeExecutorRegistry executorRegistry;
    private final WorkflowValidationService validationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public WorkflowEngineService(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowVersionRepository versionRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowNodeExecutionRepository nodeExecutionRepository,
            WorkflowJobRepository jobRepository,
            InvoiceRepository invoiceRepository,
            NodeExecutorRegistry executorRegistry,
            WorkflowValidationService validationService,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.executionRepository = executionRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
        this.jobRepository = jobRepository;
        this.invoiceRepository = invoiceRepository;
        this.executorRegistry = executorRegistry;
        this.validationService = validationService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowExecution triggerInvoiceWorkflow(UUID invoiceId) {
        UUID orgId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        WorkflowDefinition def = definitionRepository.findByOrganizationIdAndStatus(orgId, "ACTIVE")
                .orElseGet(() -> seedDefaultWorkflowForOrganization(orgId, null));

        WorkflowVersion version = versionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(def.getId())
                .stream().findFirst()
                .orElseThrow(() -> new ApiException("No published workflow version found", HttpStatus.BAD_REQUEST, "NO_WORKFLOW_VERSION"));

        WorkflowExecution execution = new WorkflowExecution(
                orgId,
                def.getId(),
                version.getId(),
                "INVOICE",
                invoice.getId()
        );
        execution.setStatus(WorkflowExecution.Status.RUNNING);
        execution.setStartedAt(Instant.now());
        execution = executionRepository.save(execution);

        invoice.setWorkflowExecutionId(execution.getId());
        invoiceRepository.save(invoice);

        // Schedule START node
        try {
            WorkflowGraph graph = objectMapper.readValue(version.getGraphDefinitionJson(), WorkflowGraph.class);
            WorkflowNode startNode = graph.getNodes().stream()
                    .filter(n -> n.getType() == NodeType.START)
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Workflow has no START node", HttpStatus.INTERNAL_SERVER_ERROR, "CORRUPT_WORKFLOW_GRAPH"));

            executeNodeStep(execution, graph, startNode, new HashMap<>());

        } catch (Exception e) {
            log.error("Failed to parse workflow graph: {}", e.getMessage(), e);
            execution.setStatus(WorkflowExecution.Status.FAILED);
            execution.setErrorMessage("Graph execution failed: " + e.getMessage());
            executionRepository.save(execution);
        }

        return execution;
    }

    @Transactional
    public void executeNodeStep(WorkflowExecution execution, WorkflowGraph graph, WorkflowNode node, Map<String, Object> contextData) {
        String idempotencyKey = String.format("%s-%s-1", execution.getId(), node.getId());
        WorkflowNodeExecution nodeExec = nodeExecutionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> new WorkflowNodeExecution(execution.getId(), node.getId(), node.getType().name(), idempotencyKey));

        nodeExec.setStatus(WorkflowNodeExecution.Status.RUNNING);
        nodeExec.setStartedAt(Instant.now());
        nodeExec = nodeExecutionRepository.save(nodeExec);

        WorkflowNodeExecutor executor = executorRegistry.getExecutor(node.getType());
        NodeExecutionContext ctx = new NodeExecutionContext(
                execution.getId(),
                execution.getOrganizationId(),
                execution.getBusinessReferenceId(),
                node.getId(),
                node.getType().name(),
                contextData
        );

        long startMs = System.currentTimeMillis();
        NodeExecutionResult result = executor.execute(ctx, node.getConfig());
        long duration = System.currentTimeMillis() - startMs;

        nodeExec.setDurationMs(duration);
        nodeExec.setCompletedAt(Instant.now());
        nodeExec.setStatus(result.getStatus());
        nodeExec.setOutputSnapshot(result.getOutputSnapshot());
        nodeExec.setErrorDetails(result.getErrorDetails());
        nodeExecutionRepository.save(nodeExec);

        if (result.getStatus() == WorkflowNodeExecution.Status.WAITING_APPROVAL) {
            execution.setStatus(WorkflowExecution.Status.WAITING_APPROVAL);
            executionRepository.save(execution);
            log.info("Workflow execution {} paused waiting for approval/review at node {}", execution.getId(), node.getId());
            return;
        }

        if (result.getStatus() == WorkflowNodeExecution.Status.FAILED) {
            execution.setStatus(WorkflowExecution.Status.FAILED);
            execution.setErrorCode("NODE_FAILED");
            execution.setErrorMessage("Node " + node.getId() + " failed: " + result.getErrorDetails());
            execution.setCompletedAt(Instant.now());
            executionRepository.save(execution);
            return;
        }

        // Terminal node check
        if (node.getType() == NodeType.END) {
            execution.setStatus(WorkflowExecution.Status.COMPLETED);
            execution.setCompletedAt(Instant.now());
            executionRepository.save(execution);
            auditService.recordSystemEvent(execution.getOrganizationId(), "WORKFLOW_COMPLETED", "WORKFLOW_EXECUTION",
                    execution.getId().toString(), "RUNNING", "COMPLETED");
            return;
        }

        // Advance to next node(s)
        List<WorkflowEdge> outgoing = graph.getOutgoingEdges(node.getId());
        for (WorkflowEdge edge : outgoing) {
            if (result.getSelectedBranch() != null) {
                if (edge.getConditionBranch() != null && !edge.getConditionBranch().equalsIgnoreCase(result.getSelectedBranch())) {
                    continue; // Skip branch
                }
            }

            WorkflowNode nextNode = graph.findNode(edge.getTarget());
            if (nextNode != null) {
                executeNodeStep(execution, graph, nextNode, contextData);
            }
        }
    }

    @Transactional
    public void resumeWorkflowAfterApproval(UUID workflowExecutionId) {
        WorkflowExecution execution = executionRepository.findById(workflowExecutionId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", workflowExecutionId));

        if (execution.getStatus() != WorkflowExecution.Status.WAITING_APPROVAL) {
            return;
        }

        WorkflowVersion version = versionRepository.findById(execution.getWorkflowVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowVersion", execution.getWorkflowVersionId()));

        try {
            WorkflowGraph graph = objectMapper.readValue(version.getGraphDefinitionJson(), WorkflowGraph.class);
            execution.setStatus(WorkflowExecution.Status.RUNNING);
            executionRepository.save(execution);

            // Complete the waiting node execution and advance down outgoing edges
            List<WorkflowNodeExecution> waitingNodes = nodeExecutionRepository.findByWorkflowExecutionIdOrderByCreatedAtAsc(workflowExecutionId)
                    .stream()
                    .filter(ne -> ne.getStatus() == WorkflowNodeExecution.Status.WAITING_APPROVAL)
                    .toList();

            for (WorkflowNodeExecution waitingNode : waitingNodes) {
                waitingNode.setStatus(WorkflowNodeExecution.Status.COMPLETED);
                waitingNode.setCompletedAt(Instant.now());
                nodeExecutionRepository.save(waitingNode);

                WorkflowNode node = graph.findNode(waitingNode.getNodeId());
                if (node != null) {
                    List<WorkflowEdge> edges = graph.getOutgoingEdges(node.getId());
                    for (WorkflowEdge edge : edges) {
                        WorkflowNode next = graph.findNode(edge.getTarget());
                        if (next != null) {
                            executeNodeStep(execution, graph, next, new HashMap<>());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to resume workflow {}: {}", workflowExecutionId, e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelWorkflowExecution(UUID workflowExecutionId, String reason) {
        WorkflowExecution execution = executionRepository.findById(workflowExecutionId).orElse(null);
        if (execution != null && (execution.getStatus() == WorkflowExecution.Status.RUNNING || execution.getStatus() == WorkflowExecution.Status.WAITING_APPROVAL)) {
            execution.setStatus(WorkflowExecution.Status.CANCELLED);
            execution.setErrorMessage("Execution cancelled: " + reason);
            execution.setCompletedAt(Instant.now());
            executionRepository.save(execution);
            auditService.recordSystemEvent(execution.getOrganizationId(), "WORKFLOW_CANCELLED", "WORKFLOW_EXECUTION",
                    execution.getId().toString(), "WAITING_APPROVAL", "CANCELLED");
        }
    }

    @Transactional
    public WorkflowExecution startWorkflow(UUID workflowDefinitionId, UUID invoiceId) {
        UUID orgId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        WorkflowDefinition def = definitionRepository.findByIdAndOrganizationId(workflowDefinitionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", workflowDefinitionId));

        WorkflowVersion version = versionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(def.getId())
                .stream().findFirst()
                .orElseThrow(() -> new ApiException("No published workflow version found for workflow: " + def.getName(), HttpStatus.BAD_REQUEST, "NO_WORKFLOW_VERSION"));

        WorkflowExecution execution = new WorkflowExecution(
                orgId,
                def.getId(),
                version.getId(),
                "INVOICE",
                invoice.getId()
        );
        execution.setStatus(WorkflowExecution.Status.RUNNING);
        execution.setStartedAt(Instant.now());
        execution = executionRepository.save(execution);

        invoice.setWorkflowExecutionId(execution.getId());
        invoiceRepository.save(invoice);

        try {
            WorkflowGraph graph = objectMapper.readValue(version.getGraphDefinitionJson(), WorkflowGraph.class);
            WorkflowNode startNode = graph.getNodes().stream()
                    .filter(n -> n.getType() == NodeType.START)
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Workflow has no START node", HttpStatus.INTERNAL_SERVER_ERROR, "CORRUPT_WORKFLOW_GRAPH"));

            executeNodeStep(execution, graph, startNode, new HashMap<>());
        } catch (Exception e) {
            log.error("Failed to execute workflow: {}", e.getMessage(), e);
            execution.setStatus(WorkflowExecution.Status.FAILED);
            execution.setErrorMessage("Graph execution failed: " + e.getMessage());
            executionRepository.save(execution);
        }

        return execution;
    }

    @Transactional
    public WorkflowExecution retryExecution(UUID executionId) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowExecution execution = executionRepository.findByIdAndOrganizationId(executionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", executionId));

        if (execution.getStatus() != WorkflowExecution.Status.FAILED) {
            throw new ApiException("Cannot retry an execution with status " + execution.getStatus() + ". Only FAILED executions can be retried.",
                    HttpStatus.CONFLICT, "EXECUTION_NOT_FAILED");
        }

        WorkflowVersion version = versionRepository.findById(execution.getWorkflowVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowVersion", execution.getWorkflowVersionId()));

        try {
            WorkflowGraph graph = objectMapper.readValue(version.getGraphDefinitionJson(), WorkflowGraph.class);
            List<WorkflowNodeExecution> nodeExecs = nodeExecutionRepository.findByWorkflowExecutionIdOrderByCreatedAtAsc(execution.getId());

            WorkflowNodeExecution failedNodeExec = nodeExecs.stream()
                    .filter(ne -> ne.getStatus() == WorkflowNodeExecution.Status.FAILED)
                    .reduce((first, second) -> second)
                    .orElse(null);

            WorkflowNode targetNode = null;
            if (failedNodeExec != null) {
                targetNode = graph.findNode(failedNodeExec.getNodeId());
            }
            if (targetNode == null) {
                targetNode = graph.getNodes().stream().filter(n -> n.getType() == NodeType.START).findFirst().orElse(null);
            }

            if (targetNode == null) {
                throw new ApiException("Cannot determine retry node in workflow graph", HttpStatus.INTERNAL_SERVER_ERROR, "CORRUPT_WORKFLOW_GRAPH");
            }

            execution.setStatus(WorkflowExecution.Status.RUNNING);
            execution.setErrorCode(null);
            execution.setErrorMessage(null);
            executionRepository.save(execution);

            auditService.recordSystemEvent(orgId, "WORKFLOW_RETRY_INITIATED", "WORKFLOW_EXECUTION",
                    execution.getId().toString(), "FAILED", "RUNNING");

            executeNodeStep(execution, graph, targetNode, new HashMap<>());
            return execution;
        } catch (Exception e) {
            log.error("Failed to retry execution {}: {}", executionId, e.getMessage(), e);
            throw new ApiException("Failed to retry execution: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "RETRY_FAILED");
        }
    }

    @Transactional
    public WorkflowExecution replayExecution(UUID executionId) {
        UUID orgId = TenantContext.getCurrentTenant();
        WorkflowExecution execution = executionRepository.findByIdAndOrganizationId(executionId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", executionId));

        auditService.recordSystemEvent(orgId, "WORKFLOW_REPLAY_REQUESTED", "WORKFLOW_EXECUTION",
                execution.getId().toString(), execution.getStatus().name(), "REPLAY");

        return startWorkflow(execution.getWorkflowDefinitionId(), execution.getBusinessReferenceId());
    }

    @Transactional
    public WorkflowDefinition seedDefaultWorkflowForOrganization(UUID organizationId, UUID createdBy) {
        WorkflowDefinition def = new WorkflowDefinition(
                organizationId,
                "Standard Accounts Payable Pipeline",
                "Automated intake, AI extraction, deterministic checks, PO matching, human approval, and export.",
                createdBy
        );
        def.setStatus("ACTIVE");
        def = definitionRepository.save(def);

        List<WorkflowNode> nodes = List.of(
                new WorkflowNode("start-1", NodeType.START, "Start Intake", null),
                new WorkflowNode("extract-1", NodeType.AI_EXTRACTION, "AI Invoice Extraction", null),
                new WorkflowNode("validate-1", NodeType.VALIDATE_INVOICE, "Deterministic Validation", null),
                new WorkflowNode("match-po-1", NodeType.MATCH_PURCHASE_ORDER, "Match Purchase Order", null),
                new WorkflowNode("check-rules-1", NodeType.CONDITION, "All Checks Passed?", null),
                new WorkflowNode("approval-1", NodeType.APPROVAL, "Human Approval Task", null),
                new WorkflowNode("review-1", NodeType.MANUAL_REVIEW, "Operator Manual Review", null),
                new WorkflowNode("export-1", NodeType.EXPORT_INVOICE, "Export Invoice", null),
                new WorkflowNode("end-1", NodeType.END, "End Pipeline", null)
        );

        List<WorkflowEdge> edges = List.of(
                new WorkflowEdge("start-1", "extract-1"),
                new WorkflowEdge("extract-1", "validate-1"),
                new WorkflowEdge("validate-1", "match-po-1"),
                new WorkflowEdge("match-po-1", "check-rules-1"),
                new WorkflowEdge("check-rules-1", "approval-1", "TRUE"),
                new WorkflowEdge("check-rules-1", "review-1", "FALSE"),
                new WorkflowEdge("approval-1", "export-1"),
                new WorkflowEdge("review-1", "approval-1"),
                new WorkflowEdge("export-1", "end-1")
        );

        WorkflowGraph graph = new WorkflowGraph(nodes, edges);
        try {
            String json = objectMapper.writeValueAsString(graph);
            WorkflowVersion version = new WorkflowVersion(def.getId(), 1, json, createdBy);
            version.setValidationResultJson("{\"valid\":true,\"errors\":[]}");
            versionRepository.save(version);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize default workflow graph", e);
        }

        return def;
    }
}
