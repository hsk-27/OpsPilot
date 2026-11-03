package com.opspilot.export.provider;

import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;

import java.util.List;

public interface AccountingExportProvider {

    String getFormat();

    ExportResult exportInvoice(Invoice invoice, List<InvoiceLine> lines, String destination);

    class ExportResult {
        private final boolean success;
        private final String payloadSummary;
        private final String errorMessage;

        public ExportResult(boolean success, String payloadSummary, String errorMessage) {
            this.success = success;
            this.payloadSummary = payloadSummary;
            this.errorMessage = errorMessage;
        }

        public static ExportResult success(String summary) {
            return new ExportResult(true, summary, null);
        }

        public static ExportResult failure(String errorMessage) {
            return new ExportResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getPayloadSummary() {
            return payloadSummary;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
