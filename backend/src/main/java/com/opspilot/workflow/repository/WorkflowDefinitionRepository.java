package com.opspilot.workflow.repository;

import com.opspilot.workflow.entity.WorkflowDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {
    Page<WorkflowDefinition> findByOrganizationId(UUID organizationId, Pageable pageable);
    Optional<WorkflowDefinition> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<WorkflowDefinition> findByOrganizationIdAndStatus(UUID organizationId, String status);
}
