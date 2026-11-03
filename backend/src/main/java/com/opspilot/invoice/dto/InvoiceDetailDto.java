package com.opspilot.invoice.dto;

import com.opspilot.invoice.entity.Invoice;
import com.opspilot.supplier.dto.SupplierDto;
import com.opspilot.validation.dto.PoMatchResult;
import com.opspilot.validation.dto.ValidationResult;

import java.util.List;
import java.util.stream.Collectors;

public class InvoiceDetailDto extends InvoiceDto {
    private List<InvoiceLineDto> lines;
    private SupplierDto supplier;
    private ValidationResult validationResult;
    private PoMatchResult poMatchResult;
    private String extractedData;
    private String originalFilename;
    private Long fileSize;

    public InvoiceDetailDto() {}

    public static InvoiceDetailDto from(Invoice inv, SupplierDto supplier, ValidationResult valRes, PoMatchResult poRes, String filename, Long size) {
        InvoiceDetailDto dto = new InvoiceDetailDto();
        dto.setId(inv.getId());
        dto.setOrganizationId(inv.getOrganizationId());
        dto.setSupplierId(inv.getSupplierId());
        dto.setSupplierName(supplier != null ? supplier.getName() : null);
        dto.setPurchaseOrderId(inv.getPurchaseOrderId());
        dto.setInvoiceNumber(inv.getInvoiceNumber());
        dto.setInvoiceDate(inv.getInvoiceDate());
        dto.setDueDate(inv.getDueDate());
        dto.setCurrency(inv.getCurrency());
        dto.setSubtotal(inv.getSubtotal());
        dto.setTaxAmount(inv.getTaxAmount());
        dto.setTotalAmount(inv.getTotalAmount());
        dto.setValidationStatus(inv.getValidationStatus());
        dto.setApprovalStatus(inv.getApprovalStatus());
        dto.setProcessingStatus(inv.getProcessingStatus());
        dto.setDocumentId(inv.getDocumentId());
        dto.setWorkflowExecutionId(inv.getWorkflowExecutionId());
        dto.setCreatedAt(inv.getCreatedAt());
        dto.setUpdatedAt(inv.getUpdatedAt());

        if (inv.getLines() != null) {
            dto.setLines(inv.getLines().stream().map(InvoiceLineDto::from).collect(Collectors.toList()));
        }
        dto.setSupplier(supplier);
        dto.setValidationResult(valRes);
        dto.setPoMatchResult(poRes);
        dto.setExtractedData(inv.getExtractedData());
        dto.setOriginalFilename(filename);
        dto.setFileSize(size);

        return dto;
    }

    public List<InvoiceLineDto> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLineDto> lines) {
        this.lines = lines;
    }

    public SupplierDto getSupplier() {
        return supplier;
    }

    public void setSupplier(SupplierDto supplier) {
        this.supplier = supplier;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public PoMatchResult getPoMatchResult() {
        return poMatchResult;
    }

    public void setPoMatchResult(PoMatchResult poMatchResult) {
        this.poMatchResult = poMatchResult;
    }

    public String getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(String extractedData) {
        this.extractedData = extractedData;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
