package com.opspilot.purchaseorder.repository;

import com.opspilot.purchaseorder.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    Page<PurchaseOrder> findByOrganizationId(UUID organizationId, Pageable pageable);
    Optional<PurchaseOrder> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<PurchaseOrder> findByOrganizationIdAndPurchaseOrderNumber(UUID organizationId, String purchaseOrderNumber);
    boolean existsByOrganizationIdAndPurchaseOrderNumber(UUID organizationId, String purchaseOrderNumber);
}
