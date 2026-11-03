package com.opspilot.document.repository;

import com.opspilot.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Document> findByOrganizationIdAndChecksumSha256(UUID organizationId, String checksumSha256);
}
