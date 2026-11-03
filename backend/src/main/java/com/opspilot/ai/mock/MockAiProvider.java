package com.opspilot.ai.mock;

import com.opspilot.ai.provider.AiProvider;
import com.opspilot.ai.provider.AiProviderMetadata;
import com.opspilot.ai.provider.InvoiceExtractionRequest;
import com.opspilot.ai.provider.InvoiceExtractionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MockAiProvider implements AiProvider {

    private static final Pattern INV_NUM_PATTERN = Pattern.compile("(?im)^[ \\t]*(?:Invoice(?:[ \\t]+(?:Number|No|#))?|Inv|Bill)[ \\t]*[:#][ \\t]*([A-Za-z0-9-_]+)");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("(?:Total|Grand Total|Amount Due)[\\s:$]*([0-9]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})");
    private static final Pattern SUPPLIER_PATTERN = Pattern.compile("(?i)(?:From|Supplier|Vendor)[ \\t]*:[ \\t]*([A-Za-z0-9 ,.'&-]+)");

    @Override
    public InvoiceExtractionResult extractInvoice(InvoiceExtractionRequest request) {
        long start = System.currentTimeMillis();
        String text = request.getDocumentText();
        String filename = request.getOriginalFilename() != null ? request.getOriginalFilename() : "";

        // Simulated error conditions for testing
        if (text != null && text.contains("SIMULATE_PROVIDER_ERROR")) {
            return InvoiceExtractionResult.failure("Simulated AI provider service unavailable (HTTP 503)", 150L);
        }
        if (text != null && text.contains("SIMULATE_MALFORMED_OUTPUT")) {
            InvoiceExtractionResult malformed = new InvoiceExtractionResult();
            malformed.setInvoiceNumber(null);
            malformed.setTotalAmount(new BigDecimal("-50.00")); // negative amount
            malformed.setLatencyMs(120L);
            return malformed;
        }

        InvoiceExtractionResult result = new InvoiceExtractionResult();
        Map<String, Double> conf = new HashMap<>();
        Map<String, String> evidence = new HashMap<>();

        boolean lowConfidenceSimulation = text != null && text.contains("SIMULATE_LOW_CONFIDENCE");
        double baseConfidence = lowConfidenceSimulation ? 0.65 : 0.96;

        // 1. Invoice Number Extraction
        String invNumber = "INV-" + Math.abs(filename.hashCode() % 10000);
        if (text != null) {
            Matcher m = INV_NUM_PATTERN.matcher(text);
            if (m.find()) {
                invNumber = m.group(1).trim();
                evidence.put("invoiceNumber", m.group(0));
            }
        }
        result.setInvoiceNumber(invNumber);
        conf.put("invoiceNumber", baseConfidence + (lowConfidenceSimulation ? 0.05 : 0.02));

        // 2. Supplier Extraction
        String supplierName = "Acme Supplies Ltd";
        if (text != null) {
            Matcher m = SUPPLIER_PATTERN.matcher(text);
            if (m.find()) {
                supplierName = m.group(1).split("\n")[0].trim();
                evidence.put("supplierName", m.group(0));
            }
        }
        result.setSupplierName(supplierName);
        conf.put("supplierName", baseConfidence + (lowConfidenceSimulation ? 0.02 : 0.03));

        // 3. Invoice Dates
        LocalDate invDate = LocalDate.now();
        if (text != null) {
            Matcher m = DATE_PATTERN.matcher(text);
            if (m.find()) {
                try {
                    invDate = LocalDate.parse(m.group(1));
                    evidence.put("invoiceDate", m.group(1));
                } catch (Exception ignored) {}
            }
        }
        result.setInvoiceDate(invDate);
        result.setDueDate(invDate.plusDays(30));
        conf.put("invoiceDate", baseConfidence + (lowConfidenceSimulation ? 0.03 : 0.02));

        // 4. Financial Amounts
        BigDecimal total = new BigDecimal("1180.00");
        if (text != null) {
            Matcher m = TOTAL_PATTERN.matcher(text);
            if (m.find()) {
                try {
                    total = new BigDecimal(m.group(1));
                    evidence.put("totalAmount", m.group(0));
                } catch (Exception ignored) {}
            }
        }

        BigDecimal subtotal = total.multiply(new BigDecimal("100")).divide(new BigDecimal("118"), 2, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(subtotal);

        result.setCurrency("USD");
        result.setSubtotal(subtotal);
        result.setTaxAmount(tax);
        result.setTotalAmount(total);
        conf.put("totalAmount", baseConfidence + (lowConfidenceSimulation ? 0.04 : 0.03));

        // 5. Line items
        result.getLineItems().add(new InvoiceExtractionResult.ExtractedLineItem(
                "Standard operational goods / consulting services",
                BigDecimal.ONE,
                subtotal,
                new BigDecimal("18.00"),
                subtotal
        ));

        result.setFieldConfidence(conf);
        result.setEvidence(evidence);
        result.setLatencyMs(System.currentTimeMillis() - start + 85L);
        result.setInputTokens(320);
        result.setOutputTokens(140);
        result.setEstimatedCost(new BigDecimal("0.000450"));
        result.setSuccess(true);

        return result;
    }

    @Override
    public AiProviderMetadata getMetadata() {
        return new AiProviderMetadata("MockAiProvider", "deterministic-mock-v1", false);
    }
}
