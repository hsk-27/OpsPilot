package com.opspilot.workflow.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.opspilot.notification.entity.NotificationType;
import com.opspilot.notification.service.NotificationService;
import com.opspilot.workflow.model.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationNodeExecutor implements WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(NotificationNodeExecutor.class);

    private final NotificationService notificationService;

    public NotificationNodeExecutor(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public NodeType supportedType() {
        return NodeType.NOTIFICATION;
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context, JsonNode configuration) {
        try {
            String title = configuration != null && configuration.has("title")
                    ? configuration.get("title").asText("OpsPilot Workflow Alert")
                    : "OpsPilot Workflow Notification";

            String message = configuration != null && configuration.has("message")
                    ? configuration.get("message").asText("Workflow execution update for invoice " + context.getInvoiceId())
                    : "Execution update for invoice: " + context.getInvoiceId();

            String typeStr = configuration != null && configuration.has("notificationType")
                    ? configuration.get("notificationType").asText("SYSTEM")
                    : "SYSTEM";

            NotificationType type;
            try {
                type = NotificationType.valueOf(typeStr.toUpperCase());
            } catch (Exception e) {
                type = NotificationType.SYSTEM;
            }

            String linkUrl = configuration != null && configuration.has("linkUrl")
                    ? configuration.get("linkUrl").asText("/invoices/" + context.getInvoiceId())
                    : "/invoices/" + context.getInvoiceId();

            UUID targetUserId = null;
            if (configuration != null && configuration.has("userId") && !configuration.get("userId").isNull()) {
                try {
                    targetUserId = UUID.fromString(configuration.get("userId").asText());
                } catch (Exception ignored) {}
            }

            notificationService.sendNotification(
                    context.getOrganizationId(),
                    targetUserId,
                    title,
                    message,
                    type,
                    linkUrl
            );

            log.info("Workflow notification sent for execution={}, node={}", context.getWorkflowExecutionId(), context.getNodeId());
            String snapshot = String.format("{\"title\":\"%s\",\"type\":\"%s\",\"targetUserId\":\"%s\"}",
                    title, type, targetUserId != null ? targetUserId : "ALL");

            return NodeExecutionResult.completed(snapshot);
        } catch (Exception e) {
            log.error("Failed to execute Notification node: {}", e.getMessage(), e);
            return NodeExecutionResult.failed("Notification delivery failed: " + e.getMessage());
        }
    }
}
