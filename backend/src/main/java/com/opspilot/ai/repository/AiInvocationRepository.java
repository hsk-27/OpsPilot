package com.opspilot.ai.repository;

import com.opspilot.ai.entity.AiInvocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiInvocationRepository extends JpaRepository<AiInvocation, UUID> {
    List<AiInvocation> findByOrganizationId(UUID organizationId);
    List<AiInvocation> findByInvoiceId(UUID invoiceId);

    @Query("SELECT COUNT(a) FROM AiInvocation a WHERE a.organizationId = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT AVG(a.latencyMs) FROM AiInvocation a WHERE a.organizationId = :orgId")
    Double getAverageLatency(@Param("orgId") UUID orgId);

    @Query("SELECT SUM(a.inputTokens) FROM AiInvocation a WHERE a.organizationId = :orgId")
    Long getTotalInputTokens(@Param("orgId") UUID orgId);

    @Query("SELECT SUM(a.outputTokens) FROM AiInvocation a WHERE a.organizationId = :orgId")
    Long getTotalOutputTokens(@Param("orgId") UUID orgId);

    @Query("SELECT SUM(a.estimatedCost) FROM AiInvocation a WHERE a.organizationId = :orgId")
    BigDecimal getTotalEstimatedCost(@Param("orgId") UUID orgId);
}
