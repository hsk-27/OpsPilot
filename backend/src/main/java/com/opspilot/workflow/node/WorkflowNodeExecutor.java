package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.workflow.model.NodeType;

public interface WorkflowNodeExecutor {
    NodeType supportedType();
    NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration);
}
