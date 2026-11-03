package com.opspilot.ai.provider;

import java.util.UUID;

public class InvoiceExtractionRequest {
    private final String documentText;
    private final String originalFilename;
    private final UUID organizationId;
    private final UUID invoiceId;

    public InvoiceExtractionRequest(String documentText, String originalFilename, UUID organizationId, UUID invoiceId) {
        this.documentText = documentText;
        this.originalFilename = originalFilename;
        this.organizationId = organizationId;
        this.invoiceId = invoiceId;
    }

    public String getDocumentText() {
        return documentText;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }
}
