package com.opspilot.approval.repository;

import com.opspilot.approval.entity.ApprovalTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, UUID> {
    Page<ApprovalTask> findByOrganizationIdAndStatus(UUID organizationId, ApprovalTask.Status status, Pageable pageable);
    List<ApprovalTask> findByInvoiceId(UUID invoiceId);
    Optional<ApprovalTask> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<ApprovalTask> findByWorkflowExecutionIdAndStatus(UUID workflowExecutionId, ApprovalTask.Status status);
    long countByOrganizationIdAndStatus(UUID organizationId, ApprovalTask.Status status);
}
