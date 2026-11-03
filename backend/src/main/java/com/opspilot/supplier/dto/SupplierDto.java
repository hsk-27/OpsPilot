package com.opspilot.supplier.dto;

import com.opspilot.supplier.entity.Supplier;
import java.time.Instant;
import java.util.UUID;

public class SupplierDto {
    private UUID id;
    private UUID organizationId;
    private String supplierCode;
    private String name;
    private String taxIdentifier;
    private String email;
    private String address;
    private String defaultCurrency;
    private String status;
    private Instant createdAt;

    public SupplierDto() {}

    public SupplierDto(UUID id, UUID organizationId, String supplierCode, String name, String taxIdentifier, String email, String address, String defaultCurrency, String status, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.supplierCode = supplierCode;
        this.name = name;
        this.taxIdentifier = taxIdentifier;
        this.email = email;
        this.address = address;
        this.defaultCurrency = defaultCurrency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static SupplierDto from(Supplier s) {
        return new SupplierDto(
                s.getId(),
                s.getOrganizationId(),
                s.getSupplierCode(),
                s.getName(),
                s.getTaxIdentifier(),
                s.getEmail(),
                s.getAddress(),
                s.getDefaultCurrency(),
                s.getStatus(),
                s.getCreatedAt()
        );
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
}
