package com.opspilot.notification.dto;

import com.opspilot.notification.entity.Notification;
import com.opspilot.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public class NotificationDto {

    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private String linkUrl;
    private Instant createdAt;

    public NotificationDto() {}

    public static NotificationDto from(Notification entity) {
        if (entity == null) return null;
        NotificationDto dto = new NotificationDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setUserId(entity.getUserId());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setType(entity.getType());
        dto.setRead(entity.isRead());
        dto.setLinkUrl(entity.getLinkUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
