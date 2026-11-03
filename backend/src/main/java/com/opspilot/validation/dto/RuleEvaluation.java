package com.opspilot.validation.dto;

public class RuleEvaluation {
    private String ruleName;
    private boolean passed;
    private String severity; // ERROR, WARNING
    private String message;
    private String field;

    public RuleEvaluation() {}

    public RuleEvaluation(String ruleName, boolean passed, String severity, String message, String field) {
        this.ruleName = ruleName;
        this.passed = passed;
        this.severity = severity;
        this.message = message;
        this.field = field;
    }

    public static RuleEvaluation pass(String ruleName, String message) {
        return new RuleEvaluation(ruleName, true, "INFO", message, null);
    }

    public static RuleEvaluation fail(String ruleName, String message, String field) {
        return new RuleEvaluation(ruleName, false, "ERROR", message, field);
    }

    public static RuleEvaluation warning(String ruleName, String message, String field) {
        return new RuleEvaluation(ruleName, false, "WARNING", message, field);
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
