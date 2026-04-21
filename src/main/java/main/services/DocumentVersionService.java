package main.services;

import jakarta.transaction.Transactional;
import main.entities.*;
import main.exceptions.ActiveVersionNotFoundException;
import main.exceptions.VersionNotFoundException;
import main.repositories.DocumentVersionRepository;
import main.web.VersionComparisonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentVersionService {

    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentFileService documentFileService;
    private final AuditLogService auditLogService;

    @Autowired
    public DocumentVersionService(DocumentVersionRepository documentVersionRepository, DocumentFileService documentFileService, AuditLogService auditLogService) {
        this.documentVersionRepository = documentVersionRepository;
        this.documentFileService = documentFileService;
        this.auditLogService = auditLogService;
    }

    public void saveVersion(DocumentVersion version) {
        documentVersionRepository.save(version);
    }

    public DocumentVersion createDocumentVersion(Document document, DocumentVersion lastVersion, int versionMajor, int versionMinor, int versionPatch,
                                                 String versionNumber,
                                                 String content) {
        DocumentVersion newVersion = DocumentVersion.builder()
                .document(document)
                .versionMajor(versionMajor)
                .versionMinor(versionMinor)
                .versionPatch(versionPatch)
                .versionNumber(versionNumber)
                .content(content)
                .status(VersionStatus.DRAFT)
                .createdBy(document.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .parentVersion(lastVersion)
                .isActive(false)
                .build();
        saveVersion(newVersion);
        auditLogService.createLogForDocument(document.getCreatedBy(), "CREATE DOCUMENT VERSION", document,
                newVersion, null, String.format("Created document version by %s", document.getCreatedBy().getUsername()));
        return newVersion;
    }

    @PreAuthorize("hasRole('AUTHOR')")
    public void createNewVersion(Document document, String changeType, String content, String filePath) {
        DocumentVersion lastVersion = findDocumentVersionByDocumentIdAndIsActiveIsTrue(document.getDocumentId())
                .orElseThrow(() -> new ActiveVersionNotFoundException("No active version found"));

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

        DocumentVersion newVersion = createDocumentVersion(document, lastVersion, major, minor, patch, newVersionNumber, content);

        DocumentFile uploadedFile = null;
        if (filePath != null && !filePath.isBlank()) {
            uploadedFile = documentFileService.uploadFile(filePath, document, newVersion, document.getCreatedBy());
        }

        auditLogService.createLogForDocument(document.getCreatedBy(), "CREATE NEW VERSION",
                document, newVersion, uploadedFile, "Create new version of document");
    }

    public void activateVersion(DocumentVersion version, User reviewer) {
        documentVersionRepository
                .findDocumentVersionByDocument_DocumentIdAndIsActiveIsTrue(version.getDocument().getDocumentId())
                .ifPresent(current -> {
                    current.setActive(false);
                    saveVersion(current);
                });
        version.setActive(true);
        saveVersion(version);
        auditLogService.createLogForDocumentVersion(reviewer, "ACTIVATE VERSION", version.getDocument(), version,
                "Activate new version of document");
    }

    public void rollbackToPreviousVersion(DocumentVersion version, User reviewer) {
        version.setActive(false);
        saveVersion(version);

        DocumentVersion parent = version.getParentVersion();
        if (parent != null) {
            parent.setActive(true);
            saveVersion(parent);
        } else {
            version.setStatus(VersionStatus.DRAFT);
            version.setActive(true);
            saveVersion(version);
            auditLogService.createLogForDocumentVersion(reviewer, "ROLLBACK TO DRAFT",
                    version.getDocument(), version, "First version rejected, returned to draft.");
        }
    }

    public VersionComparisonResult compareVersions(UUID version1, UUID version2) {
        DocumentVersion v1 = findById(version1);
        DocumentVersion v2 = findById(version2);

        List<String> lines1 = Arrays.asList(v1.getContent().split("\n"));
        List<String> lines2 = Arrays.asList(v2.getContent().split("\n"));

        List<String> addedLines = lines2.stream()
                .filter(line -> !lines1.contains(line))
                .collect(Collectors.toList());

        List<String> removedLines = lines1.stream()
                .filter(line -> !lines2.contains(line))
                .collect(Collectors.toList());

        //auditLogService.createLogForDocumentVersion("COMPARE VERSIONS", v2.getDocument(), null, "Compare versions of document");
        return VersionComparisonResult.builder()
                .versionNumber1(v1.getVersionNumber())
                .author1(v1.getCreatedBy().getUsername())
                .createdAt1(v1.getCreatedAt())
                .status1(v1.getStatus().name())
                .versionNumber2(v2.getVersionNumber())
                .author2(v2.getCreatedBy().getUsername())
                .createdAt2(v2.getCreatedAt())
                .status2(v2.getStatus().name())
                .addedLines(addedLines)
                .removedLines(removedLines)
                .build();
    }

    public Optional<DocumentVersion> findDocumentVersionByDocumentIdAndIsActiveIsTrue(UUID documentId) {
        return documentVersionRepository.findDocumentVersionByDocument_DocumentIdAndIsActiveIsTrue(documentId);
    }

    public DocumentVersion findById(UUID versionId) {
        return documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new VersionNotFoundException("Version not found"));
    }

    public List<DocumentVersion> getVersionHistory(UUID documentId) {
        return documentVersionRepository.findAllByDocument_DocumentIdOrderByCreatedAtAsc(documentId);
    }

    public List<DocumentVersion> findApprovedVersionsByDocumentId(UUID documentId) {
        return documentVersionRepository
                .findAllByDocument_DocumentIdAndStatusOrderByCreatedAtAsc(documentId, VersionStatus.APPROVED);
    }

}
