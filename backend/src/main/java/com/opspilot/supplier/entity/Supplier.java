package com.opspilot.supplier.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uk_org_supplier_code", columnNames = {"organization_id", "supplier_code"}))
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "supplier_code", nullable = false)
    private String supplierCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_identifier")
    private String taxIdentifier;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "default_currency", nullable = false)
    private String defaultCurrency = "USD";

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Supplier() {}

    public Supplier(UUID organizationId, String supplierCode, String name) {
        this(organizationId, supplierCode, name, null, null, null, "USD");
    }

    public Supplier(UUID organizationId, String supplierCode, String name, String taxIdentifier, String email, String address, String defaultCurrency) {
        this.organizationId = organizationId;
        this.supplierCode = supplierCode.trim();
        this.name = name.trim();
        this.taxIdentifier = taxIdentifier != null ? taxIdentifier.trim() : null;
        this.email = email != null ? email.trim() : null;
        this.address = address != null ? address.trim() : null;
        this.defaultCurrency = defaultCurrency != null ? defaultCurrency.trim().toUpperCase() : "USD";
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public void setTaxIdentifier(String taxIdentifier) {
        this.taxIdentifier = taxIdentifier;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
