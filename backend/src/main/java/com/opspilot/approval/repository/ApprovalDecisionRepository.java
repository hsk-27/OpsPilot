package com.opspilot.approval.repository;

import com.opspilot.approval.entity.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, UUID> {
    List<ApprovalDecision> findByApprovalTaskId(UUID approvalTaskId);
}
