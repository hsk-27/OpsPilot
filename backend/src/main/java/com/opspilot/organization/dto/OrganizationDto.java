package com.opspilot.organization.dto;

import com.opspilot.organization.entity.Organization;
import java.time.Instant;
import java.util.UUID;

public class OrganizationDto {
    private UUID id;
    private String name;
    private String slug;
    private String defaultCurrency;
    private String timezone;
    private String status;
    private Instant createdAt;

    public OrganizationDto() {}

    public OrganizationDto(UUID id, String name, String slug, String defaultCurrency, String timezone, String status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.defaultCurrency = defaultCurrency;
        this.timezone = timezone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static OrganizationDto from(Organization org) {
        return new OrganizationDto(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getDefaultCurrency(),
                org.getTimezone(),
                org.getStatus(),
                org.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
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
