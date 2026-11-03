package com.opspilot.admin.repository;

import com.opspilot.admin.entity.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, UUID> {
    List<IntegrationConfig> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<IntegrationConfig> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
