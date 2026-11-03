package com.opspilot.workflow.model;

public class WorkflowEdge {
    private String id;
    private String source;
    private String target;
    private String conditionBranch; // "TRUE", "FALSE", or null for default

    public WorkflowEdge() {}

    public WorkflowEdge(String source, String target) {
        this.source = source;
        this.target = target;
        this.id = source + "->" + target;
    }

    public WorkflowEdge(String source, String target, String conditionBranch) {
        this.source = source;
        this.target = target;
        this.conditionBranch = conditionBranch;
        this.id = source + "-" + (conditionBranch != null ? conditionBranch : "def") + "->" + target;
    }

    public String getId() {
        return id != null ? id : (source + "->" + target);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getConditionBranch() {
        return conditionBranch;
    }

    public void setConditionBranch(String conditionBranch) {
        this.conditionBranch = conditionBranch;
    }
}
