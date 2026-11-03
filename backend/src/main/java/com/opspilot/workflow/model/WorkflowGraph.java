package com.opspilot.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class WorkflowGraph {
    private List<WorkflowNode> nodes = new ArrayList<>();
    private List<WorkflowEdge> edges = new ArrayList<>();

    public WorkflowGraph() {}

    public WorkflowGraph(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.edges = edges != null ? edges : new ArrayList<>();
    }

    public WorkflowNode findNode(String id) {
        return nodes.stream().filter(n -> n.getId().equals(id)).findFirst().orElse(null);
    }

    public List<WorkflowEdge> getOutgoingEdges(String nodeId) {
        List<WorkflowEdge> out = new ArrayList<>();
        for (WorkflowEdge edge : edges) {
            if (edge.getSource().equals(nodeId)) {
                out.add(edge);
            }
        }
        return out;
    }

    public List<WorkflowNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<WorkflowNode> nodes) {
        this.nodes = nodes;
    }

    public List<WorkflowEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<WorkflowEdge> edges) {
        this.edges = edges;
    }
}
