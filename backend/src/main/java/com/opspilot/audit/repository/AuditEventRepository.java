package com.opspilot.audit.repository;

import com.opspilot.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    Page<AuditEvent> findByOrganizationId(UUID organizationId, Pageable pageable);
    List<AuditEvent> findByOrganizationIdAndEntityIdOrderByCreatedAtDesc(UUID organizationId, String entityId);
}
