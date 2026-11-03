package com.opspilot.workflow.node;

import com.opspilot.workflow.entity.WorkflowNodeExecution;

public class NodeExecutionResult {

    private final WorkflowNodeExecution.Status status;
    private final String outputSnapshot;
    private final String selectedBranch; // "TRUE", "FALSE", or null
    private final String errorDetails;

    public NodeExecutionResult(WorkflowNodeExecution.Status status, String outputSnapshot, String selectedBranch, String errorDetails) {
        this.status = status;
        this.outputSnapshot = outputSnapshot;
        this.selectedBranch = selectedBranch;
        this.errorDetails = errorDetails;
    }

    public static NodeExecutionResult completed(String outputSnapshot) {
        return new NodeExecutionResult(WorkflowNodeExecution.Status.COMPLETED, outputSnapshot, null, null);
    }

    public static NodeExecutionResult branched(String branch, String outputSnapshot) {
        return new NodeExecutionResult(WorkflowNodeExecution.Status.COMPLETED, outputSnapshot, branch, null);
    }

    public static NodeExecutionResult waitingApproval(String outputSnapshot) {
        return new NodeExecutionResult(WorkflowNodeExecution.Status.WAITING_APPROVAL, outputSnapshot, null, null);
    }

    public static NodeExecutionResult failed(String errorDetails) {
        return new NodeExecutionResult(WorkflowNodeExecution.Status.FAILED, null, null, errorDetails);
    }

    public WorkflowNodeExecution.Status getStatus() {
        return status;
    }

    public String getOutputSnapshot() {
        return outputSnapshot;
    }

    public String getSelectedBranch() {
        return selectedBranch;
    }

    public String getErrorDetails() {
        return errorDetails;
    }
}
