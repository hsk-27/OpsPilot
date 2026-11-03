package com.opspilot.audit.service;

import com.opspilot.audit.entity.AuditEvent;
import com.opspilot.audit.entity.OutboxEvent;
import com.opspilot.audit.repository.AuditEventRepository;
import com.opspilot.audit.repository.OutboxEventRepository;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    public AuditService(AuditEventRepository auditEventRepository, OutboxEventRepository outboxEventRepository) {
        this.auditEventRepository = auditEventRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public AuditEvent recordEvent(UUID actorUserId, String actorType, String action, String entityType, String entityId, String beforeState, String afterState) {
        UUID orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            return null; // system level without tenant
        }

        AuditEvent event = new AuditEvent(
                orgId,
                actorUserId,
                actorType != null ? actorType : "USER",
                action,
                entityType,
                entityId,
                beforeState,
                afterState,
                UUID.randomUUID().toString()
        );

        return auditEventRepository.save(event);
    }

    @Transactional
    public AuditEvent recordSystemEvent(UUID orgId, String action, String entityType, String entityId, String beforeState, String afterState) {
        AuditEvent event = new AuditEvent(
                orgId,
                null,
                "SYSTEM",
                action,
                entityType,
                entityId,
                beforeState,
                afterState,
                UUID.randomUUID().toString()
        );
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getAuditTrail(String entityId) {
        UUID orgId = TenantContext.getCurrentTenant();
        return auditEventRepository.findByOrganizationIdAndEntityIdOrderByCreatedAtDesc(orgId, entityId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditEvent> getAuditEvents(Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        Page<AuditEvent> page = auditEventRepository.findByOrganizationId(orgId, pageable);
        return PagedResponse.from(page);
    }

    @Transactional
    public void publishOutbox(String aggregateType, UUID aggregateId, String eventType, String payload) {
        UUID orgId = TenantContext.getCurrentTenant();
        if (orgId != null) {
            OutboxEvent event = new OutboxEvent(orgId, aggregateType, aggregateId, eventType, payload);
            outboxEventRepository.save(event);
        }
    }
}
