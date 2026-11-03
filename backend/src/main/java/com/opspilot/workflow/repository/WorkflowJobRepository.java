package com.opspilot.workflow.repository;

import com.opspilot.workflow.entity.WorkflowJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowJobRepository extends JpaRepository<WorkflowJob, UUID> {
    
    @Query("SELECT j FROM WorkflowJob j WHERE j.status = 'PENDING' AND j.availableAt <= :now ORDER BY j.priority DESC, j.createdAt ASC")
    List<WorkflowJob> findAvailableJobs(@Param("now") Instant now);

    @Query("SELECT j FROM WorkflowJob j WHERE j.status = 'LEASED' AND j.leaseExpiresAt < :now")
    List<WorkflowJob> findExpiredLeaseJobs(@Param("now") Instant now);

    long countByOrganizationIdAndStatus(UUID organizationId, WorkflowJob.Status status);
}
