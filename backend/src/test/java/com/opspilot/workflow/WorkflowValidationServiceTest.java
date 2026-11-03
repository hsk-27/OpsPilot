package com.opspilot.workflow;

import com.opspilot.workflow.model.NodeType;
import com.opspilot.workflow.model.WorkflowEdge;
import com.opspilot.workflow.model.WorkflowGraph;
import com.opspilot.workflow.model.WorkflowNode;
import com.opspilot.workflow.service.WorkflowValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowValidationServiceTest {

    private WorkflowValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new WorkflowValidationService();
    }

    @Test
    @DisplayName("Valid workflow DAG with START, CONDITION branches, and END is accepted")
    void testValidWorkflowAccepted() {
        List<WorkflowNode> nodes = List.of(
                new WorkflowNode("start", NodeType.START, "Start", null),
                new WorkflowNode("cond", NodeType.CONDITION, "Condition", null),
                new WorkflowNode("appr", NodeType.APPROVAL, "Approval", null),
                new WorkflowNode("rev", NodeType.MANUAL_REVIEW, "Review", null),
                new WorkflowNode("end", NodeType.END, "End", null)
        );

        List<WorkflowEdge> edges = List.of(
                new WorkflowEdge("start", "cond"),
                new WorkflowEdge("cond", "appr", "TRUE"),
                new WorkflowEdge("cond", "rev", "FALSE"),
                new WorkflowEdge("appr", "end"),
                new WorkflowEdge("rev", "end")
        );

        WorkflowGraph graph = new WorkflowGraph(nodes, edges);
        WorkflowValidationService.ValidationReport report = validationService.validate(graph);

        assertTrue(report.isValid());
        assertEquals(0, report.getErrors().size());
    }

    @Test
    @DisplayName("Workflow missing START node is rejected")
    void testMissingStartNodeRejected() {
        List<WorkflowNode> nodes = List.of(
                new WorkflowNode("extract", NodeType.AI_EXTRACTION, "Extract", null),
                new WorkflowNode("end", NodeType.END, "End", null)
        );
        List<WorkflowEdge> edges = List.of(new WorkflowEdge("extract", "end"));

        WorkflowGraph graph = new WorkflowGraph(nodes, edges);
        WorkflowValidationService.ValidationReport report = validationService.validate(graph);

        assertFalse(report.isValid());
        assertTrue(report.getErrors().stream().anyMatch(e -> e.contains("exactly one START node")));
    }

    @Test
    @DisplayName("Condition node missing FALSE branch is rejected")
    void testConditionMissingBranchRejected() {
        List<WorkflowNode> nodes = List.of(
                new WorkflowNode("start", NodeType.START, "Start", null),
                new WorkflowNode("cond", NodeType.CONDITION, "Condition", null),
                new WorkflowNode("end", NodeType.END, "End", null)
        );
        // Only TRUE branch present, FALSE branch missing!
        List<WorkflowEdge> edges = List.of(
                new WorkflowEdge("start", "cond"),
                new WorkflowEdge("cond", "end", "TRUE")
        );

        WorkflowGraph graph = new WorkflowGraph(nodes, edges);
        WorkflowValidationService.ValidationReport report = validationService.validate(graph);

        assertFalse(report.isValid());
        assertTrue(report.getErrors().stream().anyMatch(e -> e.contains("must have both 'TRUE' and 'FALSE'")));
    }
}
