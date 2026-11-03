package com.opspilot.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;

public class WorkflowNode {
    private String id;
    private NodeType type;
    private String label;
    private JsonNode config;
    private Double x;
    private Double y;

    public WorkflowNode() {}

    public WorkflowNode(String id, NodeType type, String label, JsonNode config) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.config = config;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }
}
