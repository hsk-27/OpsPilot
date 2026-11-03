package com.opspilot.workflow;

import com.opspilot.workflow.model.ConditionRule;
import com.opspilot.workflow.service.ConditionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
    }

    @Test
    @DisplayName("Evaluates string equality and numeric comparisons accurately")
    void testRuleEvaluation() {
        Map<String, Object> ctx = Map.of(
                "validationStatus", "PASSED",
                "totalAmount", new BigDecimal("5000.00"),
                "currency", "USD"
        );

        assertTrue(evaluator.evaluate(new ConditionRule("validationStatus", ConditionRule.Operator.EQUALS, "PASSED"), ctx));
        assertFalse(evaluator.evaluate(new ConditionRule("validationStatus", ConditionRule.Operator.EQUALS, "FAILED"), ctx));

        assertTrue(evaluator.evaluate(new ConditionRule("totalAmount", ConditionRule.Operator.GREATER_THAN, "1000"), ctx));
        assertFalse(evaluator.evaluate(new ConditionRule("totalAmount", ConditionRule.Operator.GREATER_THAN, "10000"), ctx));

        assertTrue(evaluator.evaluate(new ConditionRule("currency", ConditionRule.Operator.CONTAINS, "US"), ctx));
    }
}
