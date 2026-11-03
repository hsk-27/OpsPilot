package com.opspilot.workflow.repository;

import com.opspilot.workflow.entity.WorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {
    Page<WorkflowExecution> findByOrganizationId(UUID organizationId, Pageable pageable);
    Optional<WorkflowExecution> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<WorkflowExecution> findByBusinessReferenceId(UUID businessReferenceId);
    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndStatus(UUID organizationId, WorkflowExecution.Status status);
}
