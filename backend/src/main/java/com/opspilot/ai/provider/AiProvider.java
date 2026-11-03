package com.opspilot.ai.provider;

public interface AiProvider {
    InvoiceExtractionResult extractInvoice(InvoiceExtractionRequest request);
    AiProviderMetadata getMetadata();
}
