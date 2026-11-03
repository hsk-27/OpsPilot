package com.opspilot.notification;

import com.opspilot.common.tenant.TenantContext;
import com.opspilot.notification.dto.NotificationDto;
import com.opspilot.notification.email.MockEmailProvider;
import com.opspilot.notification.entity.Notification;
import com.opspilot.notification.entity.NotificationType;
import com.opspilot.notification.repository.NotificationRepository;
import com.opspilot.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationServiceTest {

    private NotificationService notificationService;
    private MockEmailProvider mockEmailProvider;
    private final Map<UUID, Notification> notificationStore = new HashMap<>();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        notificationStore.clear();
        mockEmailProvider = new MockEmailProvider();

        NotificationRepository repo = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Notification n = (Notification) args[0];
                        if (n.getId() == null) n.setId(UUID.randomUUID());
                        notificationStore.put(n.getId(), n);
                        return n;
                    }
                    if ("findByIdAndOrganizationId".equals(name)) {
                        UUID id = (UUID) args[0];
                        UUID orgId = (UUID) args[1];
                        Notification n = notificationStore.get(id);
                        if (n != null && orgId.equals(n.getOrganizationId())) {
                            return Optional.of(n);
                        }
                        return Optional.empty();
                    }
                    if ("findVisibleNotifications".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        UUID uId = (UUID) args[1];
                        return notificationStore.values().stream()
                                .filter(n -> orgId.equals(n.getOrganizationId()) && (n.getUserId() == null || n.getUserId().equals(uId)))
                                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                                .toList();
                    }
                    if ("countUnread".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        UUID uId = (UUID) args[1];
                        return notificationStore.values().stream()
                                .filter(n -> orgId.equals(n.getOrganizationId()) && (n.getUserId() == null || n.getUserId().equals(uId)) && !n.isRead())
                                .count();
                    }
                    if ("markAllAsRead".equals(name)) {
                        UUID orgId = (UUID) args[0];
                        UUID uId = (UUID) args[1];
                        int count = 0;
                        for (Notification n : notificationStore.values()) {
                            if (orgId.equals(n.getOrganizationId()) && (n.getUserId() == null || n.getUserId().equals(uId)) && !n.isRead()) {
                                n.setRead(true);
                                count++;
                            }
                        }
                        return count;
                    }
                    return null;
                }
        );

        notificationService = new NotificationService(repo, mockEmailProvider);
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Send in-app notification and verify retrieval and unread counter")
    public void testSendAndRetrieveNotifications() {
        NotificationDto created = notificationService.sendNotification(
                tenantId,
                userId,
                "Invoice Discrepancy",
                "Invoice #INV-9021 exceeded PO total",
                NotificationType.DISCREPANCY_FLAGGED,
                "/invoices/123"
        );

        assertNotNull(created.getId());
        assertEquals(tenantId, created.getOrganizationId());
        assertEquals("Invoice Discrepancy", created.getTitle());
        assertFalse(created.isRead());

        long unread = notificationService.getUnreadCount(userId);
        assertEquals(1, unread);

        List<NotificationDto> notifications = notificationService.getNotifications(userId);
        assertEquals(1, notifications.size());
        assertEquals("Invoice Discrepancy", notifications.get(0).getTitle());
    }

    @Test
    @DisplayName("Mark single notification and all notifications as read")
    public void testMarkAsRead() {
        NotificationDto n1 = notificationService.sendNotification(tenantId, userId, "Alert 1", "Msg 1", NotificationType.SYSTEM, null);
        NotificationDto n2 = notificationService.sendNotification(tenantId, userId, "Alert 2", "Msg 2", NotificationType.APPROVAL_REQUIRED, null);

        assertEquals(2, notificationService.getUnreadCount(userId));

        notificationService.markAsRead(n1.getId());
        assertEquals(1, notificationService.getUnreadCount(userId));

        notificationService.markAllAsRead(userId);
        assertEquals(0, notificationService.getUnreadCount(userId));
    }

    @Test
    @DisplayName("Dispatch mock email and verify in-memory logging")
    public void testMockEmailDelivery() {
        notificationService.sendEmailNotification("accounts@company.com", "Action Required: Approval Needed", "Please review invoice INV-100");
        assertEquals(1, mockEmailProvider.getSentEmails().size());
        assertEquals("accounts@company.com", mockEmailProvider.getSentEmails().get(0).toAddress());
        assertEquals("Action Required: Approval Needed", mockEmailProvider.getSentEmails().get(0).subject());
    }
}
