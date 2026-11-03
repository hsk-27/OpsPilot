package com.opspilot.export.provider;

import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockErpExportProvider implements AccountingExportProvider {

    @Override
    public String getFormat() {
        return "ERP_API";
    }

    @Override
    public ExportResult exportInvoice(Invoice invoice, List<InvoiceLine> lines, String destination) {
        // Simulate ERP sync (SAP / NetSuite / QuickBooks simulator)
        String summary = String.format("ERP_SYNC_OK: Synced invoice %s (Total: %s %s) to external accounting ledger at %s",
                invoice.getInvoiceNumber(), invoice.getCurrency(), invoice.getTotalAmount(), destination != null ? destination : "DEFAULT_LEDGER");
        return ExportResult.success(summary);
    }
}
