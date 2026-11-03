package com.opspilot.export.repository;

import com.opspilot.export.entity.ExportRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExportRecordRepository extends JpaRepository<ExportRecord, UUID> {
    Page<ExportRecord> findByOrganizationId(UUID organizationId, Pageable pageable);
    List<ExportRecord> findByInvoiceId(UUID invoiceId);
    Optional<ExportRecord> findByIdempotencyKey(String idempotencyKey);
    long countByOrganizationId(UUID organizationId);
}
