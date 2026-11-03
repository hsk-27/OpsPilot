package com.opspilot.workflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.workflow.model.ConditionRule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ConditionEvaluator {

    public boolean evaluate(ConditionRule rule, Map<String, Object> context) {
        if (rule == null || rule.getField() == null) {
            return true;
        }

        Object actualValue = resolveField(rule.getField(), context);
        String targetValue = rule.getValue();

        switch (rule.getOperator()) {
            case EQUALS:
                return actualValue != null && actualValue.toString().equalsIgnoreCase(targetValue);
            case NOT_EQUALS:
                return actualValue == null || !actualValue.toString().equalsIgnoreCase(targetValue);
            case CONTAINS:
                return actualValue != null && actualValue.toString().toLowerCase().contains(targetValue.toLowerCase());
            case IS_EMPTY:
                return actualValue == null || actualValue.toString().trim().isEmpty();
            case IS_NOT_EMPTY:
                return actualValue != null && !actualValue.toString().trim().isEmpty();
            case GREATER_THAN:
                return compareNumbers(actualValue, targetValue) > 0;
            case GREATER_THAN_OR_EQUAL:
                return compareNumbers(actualValue, targetValue) >= 0;
            case LESS_THAN:
                return compareNumbers(actualValue, targetValue) < 0;
            case LESS_THAN_OR_EQUAL:
                return compareNumbers(actualValue, targetValue) <= 0;
            default:
                return false;
        }
    }

    public boolean evaluateAll(JsonNode configNode, Map<String, Object> context) {
        if (configNode == null || !configNode.has("rules")) {
            // Default condition evaluation: check if validation passed
            Object status = context.get("validationStatus");
            return "PASSED".equalsIgnoreCase(String.valueOf(status));
        }

        JsonNode rulesArray = configNode.get("rules");
        if (!rulesArray.isArray() || rulesArray.isEmpty()) {
            Object status = context.get("validationStatus");
            return "PASSED".equalsIgnoreCase(String.valueOf(status));
        }

        for (JsonNode rNode : rulesArray) {
            String field = rNode.path("field").asText("");
            String opStr = rNode.path("operator").asText("EQUALS");
            String val = rNode.path("value").asText("");

            ConditionRule.Operator operator;
            try {
                operator = ConditionRule.Operator.valueOf(opStr.toUpperCase());
            } catch (Exception e) {
                operator = ConditionRule.Operator.EQUALS;
            }

            ConditionRule rule = new ConditionRule(field, operator, val);
            if (!evaluate(rule, context)) {
                return false;
            }
        }
        return true;
    }

    private Object resolveField(String path, Map<String, Object> context) {
        if (context.containsKey(path)) {
            return context.get(path);
        }
        // Aliases
        if ("validation.status".equalsIgnoreCase(path) || "validationStatus".equalsIgnoreCase(path)) {
            return context.get("validationStatus");
        }
        if ("confidence.average".equalsIgnoreCase(path) || "averageConfidence".equalsIgnoreCase(path)) {
            return context.get("averageConfidence");
        }
        if ("poMatch.outcome".equalsIgnoreCase(path) || "poMatchOutcome".equalsIgnoreCase(path)) {
            return context.get("poMatchOutcome");
        }
        return context.get(path);
    }

    private int compareNumbers(Object actual, String target) {
        if (actual == null || target == null) return -1;
        try {
            BigDecimal a = new BigDecimal(actual.toString());
            BigDecimal b = new BigDecimal(target.trim());
            return a.compareTo(b);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
