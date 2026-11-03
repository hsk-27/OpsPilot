package com.opspilot.notification.service;

import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.notification.dto.NotificationDto;
import com.opspilot.notification.email.EmailProvider;
import com.opspilot.notification.entity.Notification;
import com.opspilot.notification.entity.NotificationType;
import com.opspilot.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final EmailProvider emailProvider;

    public NotificationService(NotificationRepository notificationRepository, EmailProvider emailProvider) {
        this.notificationRepository = notificationRepository;
        this.emailProvider = emailProvider;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(UUID userId) {
        UUID orgId = TenantContext.getCurrentTenant();
        List<Notification> list = notificationRepository.findVisibleNotifications(orgId, userId);
        return list.stream().map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        UUID orgId = TenantContext.getCurrentTenant();
        return notificationRepository.countUnread(orgId, userId);
    }

    @Transactional
    public NotificationDto markAsRead(UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        Notification notification = notificationRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return NotificationDto.from(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        UUID orgId = TenantContext.getCurrentTenant();
        return notificationRepository.markAllAsRead(orgId, userId);
    }

    @Transactional
    public NotificationDto sendNotification(UUID organizationId, UUID userId, String title, String message, NotificationType type, String linkUrl) {
        Notification notification = new Notification(organizationId, userId, title, message, type, linkUrl);
        notification = notificationRepository.save(notification);
        log.info("Persisted in-app notification id={} for org={} user={} type={}",
                notification.getId(), organizationId, userId, type);
        return NotificationDto.from(notification);
    }

    public void sendEmailNotification(String toAddress, String subject, String body) {
        try {
            emailProvider.sendEmail(toAddress, subject, body);
        } catch (Exception e) {
            log.warn("Failed to dispatch email notification to {}: {}", toAddress, e.getMessage());
        }
    }
}
