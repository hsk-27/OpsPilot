package com.opspilot.document.service;

import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.document.entity.Document;
import com.opspilot.document.repository.DocumentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final DocumentRepository documentRepository;
    private final Path storageDirectory;

    public DocumentStorageService(
            DocumentRepository documentRepository,
            @Value("${opspilot.storage.document-path:./storage/documents}") String storagePathStr) {
        this.documentRepository = documentRepository;
        this.storageDirectory = Paths.get(storagePathStr).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize document storage directory: " + storageDirectory, e);
        }
    }

    @Transactional
    public Document storeDocument(MultipartFile file, UUID uploadedBy) {
        UUID orgId = TenantContext.getCurrentTenant();
        if (file.isEmpty()) {
            throw new ApiException("Cannot upload empty file", HttpStatus.BAD_REQUEST, "EMPTY_FILE");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new ApiException("Only PDF files are supported", HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE");
        }

        try {
            byte[] fileBytes = file.getBytes();

            // Validate PDF Magic Bytes: %PDF-
            if (fileBytes.length < 5 ||
                    fileBytes[0] != 0x25 || fileBytes[1] != 0x50 || fileBytes[2] != 0x44 ||
                    fileBytes[3] != 0x46 || fileBytes[4] != 0x2D) {
                throw new ApiException("Corrupted or invalid PDF header", HttpStatus.BAD_REQUEST, "INVALID_PDF_SIGNATURE");
            }

            // Calculate SHA-256 checksum
            String checksumSha256 = calculateSha256(fileBytes);

            // Generate opaque random storage key
            String storageKey = UUID.randomUUID().toString() + ".pdf";
            Path targetLocation = storageDirectory.resolve(storageKey);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Document document = new Document(
                    orgId,
                    originalFilename,
                    storageKey,
                    "application/pdf",
                    file.getSize(),
                    checksumSha256,
                    uploadedBy
            );

            return documentRepository.save(document);

        } catch (IOException ex) {
            throw new ApiException("Failed to store document: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", ex);
        }
    }

    @Transactional(readOnly = true)
    public Path getDocumentPath(UUID documentId) {
        UUID orgId = TenantContext.getCurrentTenant();
        Document doc = documentRepository.findByIdAndOrganizationId(documentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        Path filePath = storageDirectory.resolve(doc.getStorageKey()).normalize();
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Document file", doc.getStorageKey());
        }
        return filePath;
    }

    @Transactional(readOnly = true)
    public byte[] loadDocumentBytes(UUID documentId) {
        try {
            Path path = getDocumentPath(documentId);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ApiException("Failed to read document bytes", HttpStatus.INTERNAL_SERVER_ERROR, "FILE_READ_ERROR", e);
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
