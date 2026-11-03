package com.opspilot.invoice.entity;

public enum ProcessingStatus {
    RECEIVED,
    EXTRACTION_PENDING,
    EXTRACTED,
    VALIDATING,
    NEEDS_REVIEW,
    READY_FOR_APPROVAL,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    EXPORT_PENDING,
    EXPORTED,
    FAILED
}
