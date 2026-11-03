package com.opspilot.invoice.service;

import com.opspilot.audit.service.AuditService;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.response.PagedResponse;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.document.entity.Document;
import com.opspilot.document.service.DocumentStorageService;
import com.opspilot.invoice.dto.InvoiceDetailDto;
import com.opspilot.invoice.dto.InvoiceDto;
import com.opspilot.invoice.dto.InvoiceLineDto;
import com.opspilot.invoice.dto.UpdateInvoiceRequest;
import com.opspilot.invoice.entity.Invoice;
import com.opspilot.invoice.entity.InvoiceLine;
import com.opspilot.invoice.entity.ProcessingStatus;
import com.opspilot.invoice.repository.InvoiceLineRepository;
import com.opspilot.invoice.repository.InvoiceRepository;
import com.opspilot.purchaseorder.repository.PurchaseOrderRepository;
import com.opspilot.supplier.dto.SupplierDto;
import com.opspilot.supplier.repository.SupplierRepository;
import com.opspilot.validation.dto.PoMatchResult;
import com.opspilot.validation.dto.ValidationResult;
import com.opspilot.validation.service.DeterministicValidationService;
import com.opspilot.validation.service.PurchaseOrderMatchingService;
import com.opspilot.workflow.service.WorkflowEngineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DocumentStorageService documentStorageService;
    private final DeterministicValidationService validationService;
    private final PurchaseOrderMatchingService poMatchingService;
    private final WorkflowEngineService workflowEngineService;
    private final AuditService auditService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceLineRepository invoiceLineRepository,
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            DocumentStorageService documentStorageService,
            DeterministicValidationService validationService,
            PurchaseOrderMatchingService poMatchingService,
            WorkflowEngineService workflowEngineService,
            AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.documentStorageService = documentStorageService;
        this.validationService = validationService;
        this.poMatchingService = poMatchingService;
        this.workflowEngineService = workflowEngineService;
        this.auditService = auditService;
    }

    @Transactional
    public InvoiceDetailDto uploadInvoice(MultipartFile file, UUID purchaseOrderId, UUID actorUserId) {
        UUID orgId = TenantContext.getCurrentTenant();

        // 1. Store document privately
        Document document = documentStorageService.storeDocument(file, actorUserId);

        // 2. Create invoice in RECEIVED status
        String defaultInvNum = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Invoice invoice = new Invoice(orgId, defaultInvNum, document.getId());
        invoice.setPurchaseOrderId(purchaseOrderId);
        invoice = invoiceRepository.save(invoice);

        // 3. Record audit event
        auditService.recordEvent(
                actorUserId,
                "USER",
                "INVOICE_UPLOADED",
                "INVOICE",
                invoice.getId().toString(),
                null,
                "{\"documentId\":\"" + document.getId() + "\",\"filename\":\"" + document.getOriginalFilename() + "\"}"
        );

        // 4. Trigger automated workflow execution
        workflowEngineService.triggerInvoiceWorkflow(invoice.getId());

        return getInvoiceDetail(invoice.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceDto> getInvoices(ProcessingStatus status, Pageable pageable) {
        UUID orgId = TenantContext.getCurrentTenant();
        Page<Invoice> page = status != null
                ? invoiceRepository.findByOrganizationIdAndProcessingStatus(orgId, status, pageable)
                : invoiceRepository.findByOrganizationId(orgId, pageable);

        return PagedResponse.from(page.map(inv -> {
            InvoiceDto dto = InvoiceDto.from(inv);
            if (inv.getSupplierId() != null) {
                supplierRepository.findByIdAndOrganizationId(inv.getSupplierId(), orgId)
                        .ifPresent(s -> dto.setSupplierName(s.getName()));
            }
            if (inv.getPurchaseOrderId() != null) {
                purchaseOrderRepository.findByIdAndOrganizationId(inv.getPurchaseOrderId(), orgId)
                        .ifPresent(po -> dto.setPurchaseOrderNumber(po.getPurchaseOrderNumber()));
            }
            return dto;
        }));
    }

    @Transactional(readOnly = true)
    public InvoiceDetailDto getInvoiceDetail(UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        SupplierDto supplierDto = null;
        if (invoice.getSupplierId() != null) {
            supplierDto = supplierRepository.findByIdAndOrganizationId(invoice.getSupplierId(), orgId)
                    .map(SupplierDto::from).orElse(null);
        }

        ValidationResult valRes = validationService.validateInvoice(invoice);
        PoMatchResult poRes = poMatchingService.matchInvoice(invoice);

        String filename = "invoice.pdf";
        Long size = 0L;
        if (invoice.getDocumentId() != null) {
            try {
                Path p = documentStorageService.getDocumentPath(invoice.getDocumentId());
                filename = p.getFileName().toString();
                size = java.nio.file.Files.size(p);
            } catch (Exception ignored) {}
        }

        return InvoiceDetailDto.from(invoice, supplierDto, valRes, poRes, filename, size);
    }

    @Transactional
    public InvoiceDetailDto updateInvoice(UUID id, UpdateInvoiceRequest req, UUID actorUserId) {
        UUID orgId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        String before = String.format("{\"totalAmount\":%s,\"invoiceNumber\":\"%s\"}", invoice.getTotalAmount(), invoice.getInvoiceNumber());

        if (req.getInvoiceNumber() != null && !req.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber(req.getInvoiceNumber().trim());
        }
        if (req.getSupplierId() != null) {
            invoice.setSupplierId(req.getSupplierId());
        }
        if (req.getPurchaseOrderId() != null) {
            invoice.setPurchaseOrderId(req.getPurchaseOrderId());
        }
        if (req.getInvoiceDate() != null) {
            invoice.setInvoiceDate(req.getInvoiceDate());
        }
        if (req.getDueDate() != null) {
            invoice.setDueDate(req.getDueDate());
        }
        if (req.getCurrency() != null && !req.getCurrency().isBlank()) {
            invoice.setCurrency(req.getCurrency().trim().toUpperCase());
        }
        if (req.getSubtotal() != null) {
            invoice.setSubtotal(req.getSubtotal());
        }
        if (req.getTaxAmount() != null) {
            invoice.setTaxAmount(req.getTaxAmount());
        }
        if (req.getTotalAmount() != null) {
            invoice.setTotalAmount(req.getTotalAmount());
        }

        if (req.getLines() != null) {
            invoiceLineRepository.deleteAll(invoiceLineRepository.findByInvoiceId(invoice.getId()));
            invoice.clearLines();
            for (InvoiceLineDto l : req.getLines()) {
                invoice.addLine(new InvoiceLine(l.getDescription(), l.getQuantity(), l.getUnitPrice(), l.getTaxRate(), l.getLineTotal()));
            }
        }

        // Re-evaluate validation
        ValidationResult valResult = validationService.validateInvoice(invoice);
        invoice.setValidationStatus(valResult.getOverallStatus());
        if (invoice.getProcessingStatus() == ProcessingStatus.NEEDS_REVIEW && valResult.getOverallStatus() == com.opspilot.invoice.entity.ValidationStatus.PASSED) {
            invoice.setProcessingStatus(ProcessingStatus.READY_FOR_APPROVAL);
        }

        invoice = invoiceRepository.save(invoice);

        String after = String.format("{\"totalAmount\":%s,\"invoiceNumber\":\"%s\"}", invoice.getTotalAmount(), invoice.getInvoiceNumber());
        auditService.recordEvent(actorUserId, "USER", "INVOICE_UPDATED", "INVOICE", invoice.getId().toString(), before, after);

        return getInvoiceDetail(invoice.getId());
    }

    @Transactional
    public ValidationResult validateInvoice(UUID id) {
        UUID orgId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        ValidationResult res = validationService.validateInvoice(invoice);
        invoice.setValidationStatus(res.getOverallStatus());
        invoiceRepository.save(invoice);
        return res;
    }

    @Transactional
    public InvoiceDetailDto reprocessInvoice(UUID id, UUID actorUserId) {
        UUID orgId = TenantContext.getCurrentTenant();
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        invoice.setProcessingStatus(ProcessingStatus.RECEIVED);
        invoice.setValidationStatus(com.opspilot.invoice.entity.ValidationStatus.PENDING);
        invoice = invoiceRepository.save(invoice);

        auditService.recordEvent(actorUserId, "USER", "INVOICE_REPROCESSED", "INVOICE", invoice.getId().toString(), null, "Triggered reprocess");
        workflowEngineService.triggerInvoiceWorkflow(invoice.getId());

        return getInvoiceDetail(invoice.getId());
    }
}
