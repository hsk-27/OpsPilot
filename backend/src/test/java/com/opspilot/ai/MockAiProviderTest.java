package com.opspilot.ai;

import com.opspilot.ai.mock.MockAiProvider;
import com.opspilot.ai.provider.InvoiceExtractionRequest;
import com.opspilot.ai.provider.InvoiceExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MockAiProviderTest {

    private MockAiProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockAiProvider();
    }

    @Test
    @DisplayName("Deterministic mock extraction correctly parses standard text invoice")
    void testExtraction() {
        String docText = "From: Global Supplies Corp\nInvoice: INV-2026-888\nDate: 2026-10-15\nTotal Amount: 1180.00";
        InvoiceExtractionRequest req = new InvoiceExtractionRequest(docText, "test_inv.pdf", UUID.randomUUID(), UUID.randomUUID());

        InvoiceExtractionResult res = provider.extractInvoice(req);

        assertTrue(res.isSuccess());
        assertEquals("INV-2026-888", res.getInvoiceNumber());
        assertEquals("Global Supplies Corp", res.getSupplierName());
        assertEquals("USD", res.getCurrency());
        assertNotNull(res.getTotalAmount());
        assertTrue(res.getAverageConfidence() > 0.90);
    }

    @Test
    @DisplayName("Simulated error mode returns failure safely without throwing uncaught exceptions")
    void testSimulatedError() {
        String docText = "Invoice data SIMULATE_PROVIDER_ERROR test";
        InvoiceExtractionRequest req = new InvoiceExtractionRequest(docText, "err.pdf", UUID.randomUUID(), UUID.randomUUID());

        InvoiceExtractionResult res = provider.extractInvoice(req);

        assertFalse(res.isSuccess());
        assertNotNull(res.getErrorMessage());
    }
}
