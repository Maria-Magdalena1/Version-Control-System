package main.services;

import jakarta.transaction.Transactional;
import main.entities.*;
import main.exceptions.ActiveVersionNotFoundException;
import main.exceptions.DocumentNotFoundException;
import main.exceptions.InvalidVersionStatusException;
import main.exceptions.UnauthorizedException;
import main.repositories.DocumentRepository;
import main.web.DocumentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionService documentVersionService;
    private final AuditLogService auditLogService;

    //slojila sum go prosto da ne dava greshki zashtoto oshte ne sum dobavila kod za file
    DocumentFile file;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, DocumentVersionService documentVersionService, AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.documentVersionService = documentVersionService;
        this.auditLogService = auditLogService;
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

        auditLogService.createLogForDocument(author, "CREATE DOCUMENT",
                document, version, file, String.format("Created document by %s with version %s", author, version));
    }

    @PreAuthorize("hasRole('AUTHOR')")
    public void createNewVersion(UUID documentId, String changeType, String content) {
        DocumentVersion lastVersion = documentVersionService
                .findDocumentVersionByDocumentIdAndIsActiveIsTrue(documentId)
                .orElseThrow(()-> new ActiveVersionNotFoundException("No active version found"));

        int major = lastVersion.getVersionMajor();
        int minor = lastVersion.getVersionMinor();
        int patch = lastVersion.getVersionPatch();

        switch (changeType) {
            case "major":
                major++;
                minor = 0;
                patch = 0;
                break;
            case "minor":
                minor++;
                patch = 0;
                break;
            case "patch":
                patch++;
                break;
        }

        String newVersionNumber = String.format("%d.%d.%d", major, minor, patch);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        DocumentVersion newVersion = documentVersionService.createDocumentVersion(document, lastVersion, major, minor, patch, newVersionNumber, content);

        auditLogService.createLogForDocument(document.getCreatedBy(), "CREATE NEW VERSION",
                document, newVersion, file, "Create new version of document");
    }

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
                version.getDocument(), version, file, "Version " + version.getVersionNumber() + " submitted for review");
    }

    @PreAuthorize("hasRole('REVIEWER')")
    public void approveVersion(UUID versionId, User reviewer) {
        DocumentVersion version=documentVersionService.findById(versionId);

        if (version.getStatus() != VersionStatus.PENDING) {
            throw new InvalidVersionStatusException("Only pending versions can be approved");
        }

        version.setStatus(VersionStatus.APPROVED);
        documentVersionService.activateVersion(version);
        auditLogService.createLogForDocument(reviewer,"APPROVE VERSION",version.getDocument(),
               version,file,String.format("Version %s approved by %s.", version.getVersionNumber(), reviewer));
    }

    @PreAuthorize("hasRole('REVIEWER')")
    public void rejectVersion(UUID versionId, User reviewer, String reason) {
        DocumentVersion version=documentVersionService.findById(versionId);

        if (version.getStatus() != VersionStatus.PENDING) {
            throw new InvalidVersionStatusException("Only pending versions can be rejected");
        }

        version.setStatus(VersionStatus.REJECTED);
        version.setComment(reason);
        documentVersionService.rollbackToPreviousVersion(version);

        auditLogService.createLogForDocument(reviewer,"REJECT VERSION",version.getDocument(),
                version,file,String.format("Version %s rejected by %s. Reason: %s", version.getVersionNumber(), reviewer,reason));
    }

    public void saveDocument(Document document) {
        documentRepository.save(document);
    }
}
