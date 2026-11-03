package com.opspilot.supplier.repository;

import com.opspilot.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Page<Supplier> findByOrganizationId(UUID organizationId, Pageable pageable);
    Optional<Supplier> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Supplier> findByOrganizationIdAndSupplierCode(UUID organizationId, String supplierCode);
    Optional<Supplier> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
    boolean existsByOrganizationIdAndSupplierCode(UUID organizationId, String supplierCode);
}
