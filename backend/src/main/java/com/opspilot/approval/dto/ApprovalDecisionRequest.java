package com.opspilot.approval.dto;

import com.opspilot.approval.entity.DecisionType;
import jakarta.validation.constraints.NotNull;

public class ApprovalDecisionRequest {

    @NotNull(message = "Decision is required")
    private DecisionType decision;

    private String comments;

    public ApprovalDecisionRequest() {}

    public ApprovalDecisionRequest(DecisionType decision, String comments) {
        this.decision = decision;
        this.comments = comments;
    }

    public DecisionType getDecision() {
        return decision;
    }

    public void setDecision(DecisionType decision) {
        this.decision = decision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
