package com.opspilot.notification.repository;

import com.opspilot.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.organizationId = :orgId AND (n.userId = :userId OR n.userId IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findVisibleNotifications(@Param("orgId") UUID orgId, @Param("userId") UUID userId);

    @Query("SELECT n FROM Notification n WHERE n.organizationId = :orgId AND (n.userId = :userId OR n.userId IS NULL) ORDER BY n.createdAt DESC")
    Page<Notification> findVisibleNotificationsPaged(@Param("orgId") UUID orgId, @Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.organizationId = :orgId AND (n.userId = :userId OR n.userId IS NULL) AND n.read = false")
    long countUnread(@Param("orgId") UUID orgId, @Param("userId") UUID userId);

    Optional<Notification> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.organizationId = :orgId AND (n.userId = :userId OR n.userId IS NULL) AND n.read = false")
    int markAllAsRead(@Param("orgId") UUID orgId, @Param("userId") UUID userId);
}
