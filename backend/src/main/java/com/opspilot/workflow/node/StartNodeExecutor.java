package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

@Component
public class StartNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public NodeType supportedType() {
        return NodeType.START;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        return NodeExecutionResult.completed("{\"message\":\"Workflow initiated successfully\"}");
    }
}
