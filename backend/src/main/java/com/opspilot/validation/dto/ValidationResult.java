package com.opspilot.validation.dto;

import com.opspilot.invoice.entity.ValidationStatus;
import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private ValidationStatus overallStatus;
    private int rulesEvaluated;
    private int passedRules;
    private int failedRules;
    private int warningsCount;
    private List<RuleEvaluation> evaluations = new ArrayList<>();
    private List<String> reviewReasons = new ArrayList<>();

    public ValidationResult() {}

    public void addEvaluation(RuleEvaluation eval) {
        evaluations.add(eval);
        rulesEvaluated++;
        if (eval.isPassed()) {
            passedRules++;
        } else {
            if ("WARNING".equalsIgnoreCase(eval.getSeverity())) {
                warningsCount++;
            } else {
                failedRules++;
            }
            reviewReasons.add(eval.getMessage());
        }
        computeOverallStatus();
    }

    private void computeOverallStatus() {
        if (failedRules > 0) {
            overallStatus = ValidationStatus.FAILED;
        } else if (warningsCount > 0) {
            overallStatus = ValidationStatus.WARNING;
        } else {
            overallStatus = ValidationStatus.PASSED;
        }
    }

    public ValidationStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(ValidationStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public int getRulesEvaluated() {
        return rulesEvaluated;
    }

    public void setRulesEvaluated(int rulesEvaluated) {
        this.rulesEvaluated = rulesEvaluated;
    }

    public int getPassedRules() {
        return passedRules;
    }

    public void setPassedRules(int passedRules) {
        this.passedRules = passedRules;
    }

    public int getFailedRules() {
        return failedRules;
    }

    public void setFailedRules(int failedRules) {
        this.failedRules = failedRules;
    }

    public int getWarningsCount() {
        return warningsCount;
    }

    public void setWarningsCount(int warningsCount) {
        this.warningsCount = warningsCount;
    }

    public List<RuleEvaluation> getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(List<RuleEvaluation> evaluations) {
        this.evaluations = evaluations;
    }

    public List<String> getReviewReasons() {
        return reviewReasons;
    }

    public void setReviewReasons(List<String> reviewReasons) {
        this.reviewReasons = reviewReasons;
    }
}
