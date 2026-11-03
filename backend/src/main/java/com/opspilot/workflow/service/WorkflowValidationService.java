package com.opspilot.workflow.service;

import com.opspilot.workflow.model.NodeType;
import com.opspilot.workflow.model.WorkflowEdge;
import com.opspilot.workflow.model.WorkflowGraph;
import com.opspilot.workflow.model.WorkflowNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkflowValidationService {

    public ValidationReport validate(WorkflowGraph graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            errors.add("Workflow graph must contain at least one node.");
            return new ValidationReport(false, errors, warnings);
        }

        // 1. Check unique node IDs
        Set<String> nodeIds = new HashSet<>();
        List<WorkflowNode> startNodes = new ArrayList<>();
        List<WorkflowNode> endNodes = new ArrayList<>();

        for (WorkflowNode node : graph.getNodes()) {
            if (node.getId() == null || node.getId().isBlank()) {
                errors.add("Node ID cannot be empty.");
            } else if (!nodeIds.add(node.getId())) {
                errors.add("Duplicate node ID detected: " + node.getId());
            }

            if (node.getType() == NodeType.START) {
                startNodes.add(node);
            } else if (node.getType() == NodeType.END) {
                endNodes.add(node);
            }
        }

        // 2. Exactly one START node
        if (startNodes.isEmpty()) {
            errors.add("Workflow must have exactly one START node.");
        } else if (startNodes.size() > 1) {
            errors.add("Workflow contains multiple START nodes. Exactly one is permitted.");
        }

        // 3. At least one reachable END node
        if (endNodes.isEmpty()) {
            errors.add("Workflow must have at least one terminal END node.");
        }

        // 4. Validate edges reference existing nodes
        Map<String, List<WorkflowEdge>> outgoingMap = new HashMap<>();
        for (WorkflowEdge edge : graph.getEdges()) {
            if (!nodeIds.contains(edge.getSource())) {
                errors.add("Edge references non-existent source node: " + edge.getSource());
            }
            if (!nodeIds.contains(edge.getTarget())) {
                errors.add("Edge references non-existent target node: " + edge.getTarget());
            }
            outgoingMap.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
        }

        // 5. Condition node branch checks
        for (WorkflowNode node : graph.getNodes()) {
            if (node.getType() == NodeType.CONDITION) {
                List<WorkflowEdge> outs = outgoingMap.getOrDefault(node.getId(), Collections.emptyList());
                boolean hasTrue = outs.stream().anyMatch(e -> "TRUE".equalsIgnoreCase(e.getConditionBranch()));
                boolean hasFalse = outs.stream().anyMatch(e -> "FALSE".equalsIgnoreCase(e.getConditionBranch()));
                if (!hasTrue || !hasFalse) {
                    errors.add(String.format("CONDITION node '%s' must have both 'TRUE' and 'FALSE' outgoing edge branches.", node.getId()));
                }
            }
        }

        // 6. Reachability from START node
        if (startNodes.size() == 1) {
            String startId = startNodes.get(0).getId();
            Set<String> reachable = new HashSet<>();
            Queue<String> queue = new LinkedList<>();
            queue.add(startId);
            reachable.add(startId);

            while (!queue.isEmpty()) {
                String curr = queue.poll();
                for (WorkflowEdge e : outgoingMap.getOrDefault(curr, Collections.emptyList())) {
                    if (reachable.add(e.getTarget())) {
                        queue.add(e.getTarget());
                    }
                }
            }

            for (WorkflowNode node : graph.getNodes()) {
                if (!reachable.contains(node.getId())) {
                    warnings.add(String.format("Node '%s' (%s) is not reachable from START.", node.getId(), node.getType()));
                }
            }

            boolean canReachEnd = endNodes.stream().anyMatch(n -> reachable.contains(n.getId()));
            if (!canReachEnd) {
                errors.add("No END node is reachable from START node.");
            }
        }

        boolean valid = errors.isEmpty();
        return new ValidationReport(valid, errors, warnings);
    }

    public static class ValidationReport {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationReport(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
