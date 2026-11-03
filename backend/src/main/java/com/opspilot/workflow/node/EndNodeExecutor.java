package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

@Component
public class EndNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public NodeType supportedType() {
        return NodeType.END;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        return NodeExecutionResult.completed("{\"message\":\"Workflow execution completed successfully\"}");
    }
}
