package com.opspilot.workflow.repository;

import com.opspilot.workflow.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {
    List<WorkflowVersion> findByWorkflowDefinitionIdOrderByVersionNumberDesc(UUID workflowDefinitionId);
    Optional<WorkflowVersion> findByWorkflowDefinitionIdAndVersionNumber(UUID workflowDefinitionId, Integer versionNumber);

    @Query("SELECT MAX(v.versionNumber) FROM WorkflowVersion v WHERE v.workflowDefinitionId = :defId")
    Integer findMaxVersionNumber(@Param("defId") UUID workflowDefinitionId);
}
