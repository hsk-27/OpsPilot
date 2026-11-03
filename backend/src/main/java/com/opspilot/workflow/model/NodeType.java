package com.opspilot.workflow.model;

public enum NodeType {
    START,
    AI_EXTRACTION,
    VALIDATE_INVOICE,
    MATCH_PURCHASE_ORDER,
    CONDITION,
    APPROVAL,
    NOTIFICATION,
    EXPORT_INVOICE,
    MANUAL_REVIEW,
    END
}
