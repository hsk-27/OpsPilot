package com.opspilot.export.provider;

import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CsvExportProvider implements AccountingExportProvider {

    @Override
    public String getFormat() {
        return "CSV";
    }

    @Override
    public ExportResult exportInvoice(Invoice invoice, List<InvoiceLine> lines, String destination) {
        StringBuilder csv = new StringBuilder();
        csv.append("InvoiceNumber,InvoiceDate,DueDate,Currency,Subtotal,TaxAmount,TotalAmount,Status\n");
        csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getProcessingStatus()
        ));

        csv.append("\nLineItems:\nDescription,Quantity,UnitPrice,TaxRate,LineTotal\n");
        if (lines != null) {
            for (InvoiceLine l : lines) {
                csv.append(String.format("\"%s\",%s,%s,%s,%s\n",
                        l.getDescription().replace("\"", "\"\""),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getTaxRate(),
                        l.getLineTotal()
                ));
            }
        }

        return ExportResult.success(csv.toString());
    }
}
