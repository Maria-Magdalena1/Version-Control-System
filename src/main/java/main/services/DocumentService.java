package main.services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Paragraph;
import jakarta.transaction.Transactional;
import main.entities.*;
import main.exceptions.ActiveVersionNotFoundException;
import main.exceptions.DocumentNotFoundException;
import main.exceptions.InvalidVersionStatusException;
import main.exceptions.UnauthorizedException;
import main.repositories.DocumentRepository;
import main.web.DocumentDTO;
import main.web.VersionComparisonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionService documentVersionService;
    private final AuditLogService auditLogService;
    private final DocumentFileService documentFileService;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, DocumentVersionService documentVersionService, AuditLogService auditLogService, DocumentFileService documentFileService) {
        this.documentRepository = documentRepository;
        this.documentVersionService = documentVersionService;
        this.auditLogService = auditLogService;
        this.documentFileService = documentFileService;
    }

    @PreAuthorize("hasRole('AUTHOR')")
    public void create(DocumentDTO dto, User author) {
        Document document = Document.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .createdBy(author)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        saveDocument(document);

        DocumentVersion version = documentVersionService.createDocumentVersion(document, null, 1, 0, 0, "1.0.0",
                dto.getContent());

        DocumentFile uploadedFile = null;
        if (dto.getFilePath() != null && !dto.getFilePath().isBlank()) {
            uploadedFile = documentFileService.uploadFile(dto.getFilePath(), document, version, author);
        }

        auditLogService.createLogForDocument(author, "CREATE DOCUMENT",
                document, version,uploadedFile, String.format("Created document by %s with version %s", author.getUsername(), version.getVersionNumber()));
    }

//    @PreAuthorize("hasRole('AUTHOR')")
//    public void createNewVersion(UUID documentId, String changeType, String content,String filePath) {
//        DocumentVersion lastVersion = documentVersionService
//                .findDocumentVersionByDocumentIdAndIsActiveIsTrue(documentId)
//                .orElseThrow(() -> new ActiveVersionNotFoundException("No active version found"));
//
//        int major = lastVersion.getVersionMajor();
//        int minor = lastVersion.getVersionMinor();
//        int patch = lastVersion.getVersionPatch();
//
//        switch (changeType) {
//            case "major":
//                major++;
//                minor = 0;
//                patch = 0;
//                break;
//            case "minor":
//                minor++;
//                patch = 0;
//                break;
//            case "patch":
//                patch++;
//                break;
//        }
//
//        String newVersionNumber = String.format("%d.%d.%d", major, minor, patch);
//
//        Document document = documentRepository.findById(documentId)
//                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
//
//        DocumentVersion newVersion = documentVersionService.createDocumentVersion(document, lastVersion, major, minor, patch, newVersionNumber, content);
//
//        DocumentFile uploadedFile = null;
//        if (filePath != null && !filePath.isBlank()) {
//            uploadedFile = documentFileService.uploadFile(filePath, document, newVersion, document.getCreatedBy());
//        }
//
//        auditLogService.createLogForDocument(document.getCreatedBy(), "CREATE NEW VERSION",
//                document, newVersion, uploadedFile, "Create new version of document");
//    }

    @PreAuthorize("hasRole('AUTHOR')")
    public void submitForReview(UUID versionId, User author) {
        DocumentVersion version = documentVersionService.findById(versionId);

        if (!version.getCreatedBy().getUserId().equals(author.getUserId())) {
            throw new UnauthorizedException("You are not the author of this document");
        }

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new InvalidVersionStatusException("Only draft versions can be submitted for review");
        }

        version.setStatus(VersionStatus.PENDING);
        documentVersionService.saveVersion(version);

        auditLogService.createLogForDocument(author, "SUBMIT FOR REVIEW",
                version.getDocument(), version, null, "Version " + version.getVersionNumber() + " submitted for review");
    }

    @PreAuthorize("hasAnyRole('AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")
    public List<DocumentVersion> getVersionHistory(UUID documentId) {
        documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        return documentVersionService.getVersionHistory(documentId);
    }

    @PreAuthorize("hasAnyRole('AUTHOR', 'REVIEWER', 'READER', 'ADMINISTRATOR')")
    public VersionComparisonResult compareVersions(UUID versionId1, UUID versionId2) {
        return documentVersionService.compareVersions(versionId1, versionId2);
    }

    @PreAuthorize("hasAnyRole('READER', 'AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")
    public DocumentVersion getActiveVersion(UUID documentId) {
        return documentVersionService
                .findDocumentVersionByDocumentIdAndIsActiveIsTrue(documentId)
                .orElseThrow(() -> new ActiveVersionNotFoundException("No active version found"));
    }

    @PreAuthorize("hasAnyRole('READER', 'AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")
    public List<DocumentVersion> getApprovedVersions(UUID documentId) {
        documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        return documentVersionService.findApprovedVersionsByDocumentId(documentId);
    }

    @PreAuthorize("hasAnyRole('READER', 'AUTHOR', 'REVIEWER', 'ADMINISTRATOR')")
    public byte[] exportToPdf(UUID documentId) {
        DocumentVersion activeVersion = getActiveVersion(documentId);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            com.itextpdf.layout.Document pdfDocument = new com.itextpdf.layout.Document(pdf);

            pdfDocument.add(new Paragraph("Title: " + activeVersion.getDocument().getTitle()));
            pdfDocument.add(new Paragraph("Version: " + activeVersion.getVersionNumber()));
            pdfDocument.add(new Paragraph("Author: " + activeVersion.getCreatedBy().getUsername()));
            pdfDocument.add(new Paragraph("Date: " + activeVersion.getCreatedAt()));
            pdfDocument.add(new Paragraph("Status: " + activeVersion.getStatus()));
            pdfDocument.add(new Paragraph("Content: \n" + activeVersion.getContent()));

            pdfDocument.close();

            auditLogService.createLogForDocument(
                    activeVersion.getCreatedBy(),
                    "EXPORT TO PDF",
                    activeVersion.getDocument(),
                    activeVersion,
                    null,
                    String.format("Document %s exported to PDF", activeVersion.getDocument().getTitle())
            );

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to export document to PDF", e);
        }
    }

    public void saveDocument(Document document) {
        documentRepository.save(document);
    }

    public Optional<Document> findById(UUID documentId) {
        return documentRepository.findById(documentId);
    }
}
