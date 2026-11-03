package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.workflow.model.NodeType;
import com.opspilot.workflow.service.ConditionEvaluator;
import org.springframework.stereotype.Component;

@Component
public class ConditionNodeExecutor implements WorkflowNodeExecutor {

    private final ConditionEvaluator conditionEvaluator;

    public ConditionNodeExecutor(ConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.CONDITION;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        boolean passed = conditionEvaluator.evaluateAll(configuration, context.getContextData());
        String branch = passed ? "TRUE" : "FALSE";
        String snapshot = String.format("{\"evaluated\":true,\"result\":\"%s\",\"branch\":\"%s\"}", passed, branch);
        return NodeExecutionResult.branched(branch, snapshot);
    }
}
