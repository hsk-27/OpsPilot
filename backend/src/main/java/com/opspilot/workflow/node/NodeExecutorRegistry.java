package com.opspilot.workflow.node;

import com.opspilot.workflow.model.NodeType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NodeExecutorRegistry {

    private final Map<NodeType, WorkflowNodeExecutor> executorMap = new EnumMap<>(NodeType.class);

    public NodeExecutorRegistry(List<WorkflowNodeExecutor> executors) {
        for (WorkflowNodeExecutor executor : executors) {
            executorMap.put(executor.supportedType(), executor);
        }
    }

    public WorkflowNodeExecutor getExecutor(NodeType type) {
        WorkflowNodeExecutor executor = executorMap.get(type);
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for node type: " + type);
        }
        return executor;
    }
}
