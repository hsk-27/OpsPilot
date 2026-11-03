package com.opspilot.export;

import com.opspilot.audit.service.AuditService;
import com.opspilot.common.exception.ApiException;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.export.entity.ExportRecord;
import com.opspilot.export.provider.CsvExportProvider;
import com.opspilot.export.provider.JsonExportProvider;
import com.opspilot.export.provider.MockErpExportProvider;
import com.opspilot.export.repository.ExportRecordRepository;
import com.opspilot.export.service.InvoiceExportService;
import com.opspilot.invoice.entity.ApprovalStatus;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceLineRepository;
import com.opspilot.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class InvoiceExportServiceTest {

    private InvoiceRepository invoiceRepository;
    private InvoiceLineRepository invoiceLineRepository;
    private ExportRecordRepository exportRecordRepository;
    private AuditService auditService;
    private InvoiceExportService exportService;

    private final UUID tenantId = UUID.randomUUID();
    private final Map<UUID, Invoice> invoiceStore = new HashMap<>();
    private final Map<String, ExportRecord> exportStore = new HashMap<>();

    @BeforeEach
    public void setUp() {
        TenantContext.setCurrentTenant(tenantId);
        invoiceStore.clear();
        exportStore.clear();

        // InvoiceRepository proxy
        invoiceRepository = (InvoiceRepository) Proxy.newProxyInstance(
                InvoiceRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByIdAndOrganizationId")) {
                        UUID id = (UUID) args[0];
                        return Optional.ofNullable(invoiceStore.get(id));
                    }
                    if (method.getName().equals("save")) {
                        Invoice inv = (Invoice) args[0];
                        if (inv.getId() == null) inv.setId(UUID.randomUUID());
                        invoiceStore.put(inv.getId(), inv);
                        return inv;
                    }
                    return null;
                }
        );

        // InvoiceLineRepository proxy
        invoiceLineRepository = (InvoiceLineRepository) Proxy.newProxyInstance(
                InvoiceLineRepository.class.getClassLoader(),
                new Class<?>[]{InvoiceLineRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByInvoiceId")) {
                        return List.of(
                                new InvoiceLine("Server Rack Installation", BigDecimal.ONE, new BigDecimal("1200.00"), BigDecimal.ZERO, new BigDecimal("1200.00"))
                        );
                    }
                    return Collections.emptyList();
                }
        );

        // ExportRecordRepository proxy
        exportRecordRepository = (ExportRecordRepository) Proxy.newProxyInstance(
                ExportRecordRepository.class.getClassLoader(),
                new Class<?>[]{ExportRecordRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByIdempotencyKey")) {
                        String key = (String) args[0];
                        return Optional.ofNullable(exportStore.get(key));
                    }
                    if (method.getName().equals("save")) {
                        ExportRecord rec = (ExportRecord) args[0];
                        if (rec.getId() == null) rec.setId(UUID.randomUUID());
                        if (rec.getIdempotencyKey() != null) {
                            exportStore.put(rec.getIdempotencyKey(), rec);
                        }
                        return rec;
                    }
                    return null;
                }
        );

        // AuditEventRepository & OutboxEventRepository proxies
        var auditEventRepository = (com.opspilot.audit.repository.AuditEventRepository) Proxy.newProxyInstance(
                com.opspilot.audit.repository.AuditEventRepository.class.getClassLoader(),
                new Class<?>[]{com.opspilot.audit.repository.AuditEventRepository.class},
                (proxy, method, args) -> null
        );
        var outboxEventRepository = (com.opspilot.audit.repository.OutboxEventRepository) Proxy.newProxyInstance(
                com.opspilot.audit.repository.OutboxEventRepository.class.getClassLoader(),
                new Class<?>[]{com.opspilot.audit.repository.OutboxEventRepository.class},
                (proxy, method, args) -> null
        );

        auditService = new AuditService(auditEventRepository, outboxEventRepository);

        // Export providers
        var providers = List.of(
                new CsvExportProvider(),
                new JsonExportProvider(new com.fasterxml.jackson.databind.ObjectMapper()),
                new MockErpExportProvider()
        );

        exportService = new InvoiceExportService(
                invoiceRepository,
                invoiceLineRepository,
                exportRecordRepository,
                auditService,
                providers
        );
    }

    @AfterEach
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Exporting an approved invoice to CSV generates record and updates processing status")
    public void testExportApprovedInvoiceToCsv() {
        Invoice invoice = new Invoice(tenantId, "INV-2026-CSV", UUID.randomUUID());
        invoice.setId(UUID.randomUUID());
        invoice.setApprovalStatus(ApprovalStatus.APPROVED);
        invoice.setProcessingStatus(ProcessingStatus.APPROVED);
        invoice.setSubtotal(new BigDecimal("1200.00"));
        invoice.setTotalAmount(new BigDecimal("1200.00"));
        invoiceStore.put(invoice.getId(), invoice);

        ExportRecord record = exportService.exportInvoice(invoice.getId(), "CSV", "DOWNLOAD");

        assertNotNull(record);
        assertEquals("COMPLETED", record.getStatus());
        assertEquals("CSV", record.getExportFormat());
        assertTrue(record.getPayloadSummary().contains("INV-2026-CSV"));
        assertEquals(ProcessingStatus.EXPORTED, invoice.getProcessingStatus());
    }

    @Test
    @DisplayName("Exporting an approved invoice to JSON structure succeeds")
    public void testExportApprovedInvoiceToJson() {
        Invoice invoice = new Invoice(tenantId, "INV-2026-JSON", UUID.randomUUID());
        invoice.setId(UUID.randomUUID());
        invoice.setApprovalStatus(ApprovalStatus.APPROVED);
        invoice.setProcessingStatus(ProcessingStatus.APPROVED);
        invoiceStore.put(invoice.getId(), invoice);

        ExportRecord record = exportService.exportInvoice(invoice.getId(), "JSON", "LOCAL_EXPORT");

        assertNotNull(record);
        assertEquals("COMPLETED", record.getStatus());
        assertEquals("JSON", record.getExportFormat());
        assertTrue(record.getPayloadSummary().contains("\"invoiceNumber\""));
    }

    @Test
    @DisplayName("Exporting an unapproved invoice throws ApiException with INVOICE_NOT_APPROVED")
    public void testExportUnapprovedInvoiceFails() {
        Invoice invoice = new Invoice(tenantId, "INV-PENDING", UUID.randomUUID());
        invoice.setId(UUID.randomUUID());
        invoice.setApprovalStatus(ApprovalStatus.PENDING);
        invoiceStore.put(invoice.getId(), invoice);

        ApiException ex = assertThrows(ApiException.class, () ->
                exportService.exportInvoice(invoice.getId(), "CSV", "DOWNLOAD")
        );

        assertEquals("INVOICE_NOT_APPROVED", ex.getCode());
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    @DisplayName("Export is idempotent and returns existing record without duplicating")
    public void testExportIdempotency() {
        Invoice invoice = new Invoice(tenantId, "INV-IDEMPOTENT", UUID.randomUUID());
        invoice.setId(UUID.randomUUID());
        invoice.setApprovalStatus(ApprovalStatus.APPROVED);
        invoiceStore.put(invoice.getId(), invoice);

        ExportRecord first = exportService.exportInvoice(invoice.getId(), "CSV", "DOWNLOAD");
        ExportRecord second = exportService.exportInvoice(invoice.getId(), "CSV", "DOWNLOAD");

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());
        assertEquals(1, exportStore.size());
    }
}
