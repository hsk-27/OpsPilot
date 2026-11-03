package com.opspilot.workflow.node;

import java.util.Map;
import java.util.UUID;

public class NodeExecutionContext {

    private final UUID workflowExecutionId;
    private final UUID organizationId;
    private final UUID invoiceId;
    private final String nodeId;
    private final String nodeType;
    private final Map<String, Object> contextData;

    public NodeExecutionContext(UUID workflowExecutionId, UUID organizationId, UUID invoiceId, String nodeId, String nodeType, Map<String, Object> contextData) {
        this.workflowExecutionId = workflowExecutionId;
        this.organizationId = organizationId;
        this.invoiceId = invoiceId;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.contextData = contextData;
    }

    public UUID getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public Map<String, Object> getContextData() {
        return contextData;
    }
}
