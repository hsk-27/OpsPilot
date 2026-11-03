package com.opspilot.invoice.repository;

import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByOrganizationId(UUID organizationId, Pageable pageable);
    Page<Invoice> findByOrganizationIdAndProcessingStatus(UUID organizationId, ProcessingStatus status, Pageable pageable);
    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);
    
    @Query("SELECT i FROM Invoice i WHERE i.organizationId = :orgId AND i.supplierId = :supplierId AND LOWER(i.invoiceNumber) = LOWER(:invoiceNumber) AND i.currency = :currency AND i.processingStatus != 'REJECTED'")
    List<Invoice> findPotentialDuplicates(
            @Param("orgId") UUID orgId,
            @Param("supplierId") UUID supplierId,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("currency") String currency
    );

    long countByOrganizationId(UUID organizationId);
    long countByOrganizationIdAndProcessingStatus(UUID organizationId, ProcessingStatus status);
    long countByOrganizationIdAndApprovalStatus(UUID organizationId, ApprovalStatus approvalStatus);
}
