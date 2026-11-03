package com.opspilot.workflow.repository;

import com.opspilot.workflow.entity.WorkflowNodeExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowNodeExecutionRepository extends JpaRepository<WorkflowNodeExecution, UUID> {
    List<WorkflowNodeExecution> findByWorkflowExecutionIdOrderByCreatedAtAsc(UUID workflowExecutionId);
    Optional<WorkflowNodeExecution> findByIdempotencyKey(String idempotencyKey);
    Optional<WorkflowNodeExecution> findByWorkflowExecutionIdAndNodeId(UUID workflowExecutionId, String nodeId);
}
