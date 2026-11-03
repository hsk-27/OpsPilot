package com.opspilot.export.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonExportProvider implements AccountingExportProvider {

    private final ObjectMapper objectMapper;

    public JsonExportProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getFormat() {
        return "JSON";
    }

    @Override
    public ExportResult exportInvoice(Invoice invoice, List<InvoiceLine> lines, String destination) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("invoiceId", invoice.getId());
            payload.put("invoiceNumber", invoice.getInvoiceNumber());
            payload.put("supplierId", invoice.getSupplierId());
            payload.put("purchaseOrderId", invoice.getPurchaseOrderId());
            payload.put("invoiceDate", invoice.getInvoiceDate());
            payload.put("dueDate", invoice.getDueDate());
            payload.put("currency", invoice.getCurrency());
            payload.put("subtotal", invoice.getSubtotal());
            payload.put("taxAmount", invoice.getTaxAmount());
            payload.put("totalAmount", invoice.getTotalAmount());
            payload.put("lines", lines);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            return ExportResult.success(json);
        } catch (Exception e) {
            return ExportResult.failure("JSON formatting failed: " + e.getMessage());
        }
    }
}
