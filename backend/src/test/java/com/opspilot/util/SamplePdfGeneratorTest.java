package com.opspilot.util;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SamplePdfGeneratorTest {

    @Test
    void generateSamplePdfs() throws Exception {
        File outDir = new File("../samples/invoices");
        outDir.mkdirs();

        createPdf(new File(outDir, "01_acme_industrial_standard.pdf"), List.of(
                "Supplier: Acme Industrial Supplies Ltd",
                "Invoice Number: INV-2026-8801",
                "Invoice Date: 2026-11-05",
                "Purchase Order: PO-2026-9001",
                "Currency: USD",
                "Total: 11800.00"
        ));

        createPdf(new File(outDir, "02_cloudscale_hosting_approval.pdf"), List.of(
                "Supplier: CloudScale Hosting Inc",
                "Invoice Number: INV-2026-7702",
                "Invoice Date: 2026-11-06",
                "Purchase Order: PO-2026-9002",
                "Currency: USD",
                "Total: 3540.00"
        ));

        createPdf(new File(outDir, "03_apex_office_needs_review.pdf"), List.of(
                "Supplier: Apex Office Essentials",
                "Invoice Number: INV-2026-6603",
                "Invoice Date: 2026-11-07",
                "Currency: USD",
                "Total: 600.00",
                "Notice: Stated total 600.00 does not match subtotal 450 + tax 50"
        ));

        createPdf(new File(outDir, "04_simulated_low_confidence.pdf"), List.of(
                "Supplier: Acme Industrial Supplies Ltd",
                "Invoice Number: INV-2026-5504",
                "Invoice Date: 2026-11-08",
                "Total: 1180.00",
                "Flags: SIMULATE_LOW_CONFIDENCE"
        ));

        assertTrue(new File(outDir, "01_acme_industrial_standard.pdf").exists());
        assertTrue(new File(outDir, "02_cloudscale_hosting_approval.pdf").exists());
        assertTrue(new File(outDir, "03_apex_office_needs_review.pdf").exists());
        assertTrue(new File(outDir, "04_simulated_low_confidence.pdf").exists());

        com.opspilot.document.service.DocumentTextExtractor extractor = new com.opspilot.document.service.DocumentTextExtractor();
        com.opspilot.document.service.DocumentTextExtractor.ExtractedDocumentText extracted = extractor.extract(new File(outDir, "01_acme_industrial_standard.pdf").toPath());
        assertTrue(extracted.isSuccess());
        assertTrue(extracted.getText().contains("Acme Industrial Supplies Ltd"));
        assertTrue(extracted.getText().contains("INV-2026-8801"));
    }

    private void createPdf(File file, List<String> lines) throws Exception {
        StringBuilder streamContent = new StringBuilder();
        streamContent.append("BT\n/F1 12 Tf\n72 720 Td\n");
        for (String line : lines) {
            streamContent.append("(").append(escapePdf(line)).append(") Tj\n0 -20 Td\n");
        }
        streamContent.append("ET\n");

        byte[] streamBytes = streamContent.toString().getBytes(StandardCharsets.ISO_8859_1);

        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        
        long pos1 = sb.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        long pos2 = sb.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        long pos3 = sb.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");

        long pos4 = sb.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        sb.append("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        long pos5 = sb.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        sb.append("5 0 obj\n<< /Length ").append(streamBytes.length).append(" >>\nstream\n");
        sb.append(streamContent);
        sb.append("endstream\nendobj\n");

        long xrefPos = sb.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        sb.append("xref\n0 6\n");
        sb.append("0000000000 65535 f \n");
        sb.append(String.format("%010d 00000 n \n", pos1));
        sb.append(String.format("%010d 00000 n \n", pos2));
        sb.append(String.format("%010d 00000 n \n", pos3));
        sb.append(String.format("%010d 00000 n \n", pos4));
        sb.append(String.format("%010d 00000 n \n", pos5));
        sb.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n");
        sb.append(xrefPos).append("\n%%EOF\n");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private String escapePdf(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
