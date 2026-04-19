package main.services;

import jakarta.transaction.Transactional;
import main.entities.Document;
import main.entities.DocumentVersion;
import main.entities.VersionStatus;
import main.exceptions.ApprovedVersionNotFoundException;
import main.exceptions.VersionNotFoundException;
import main.repositories.DocumentVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DocumentVersionService {

    private final DocumentVersionRepository documentVersionRepository;

    @Autowired
    public DocumentVersionService(DocumentVersionRepository documentVersionRepository) {
        this.documentVersionRepository = documentVersionRepository;
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
        return newVersion;
    }

    public void activateVersion(DocumentVersion version) {
        documentVersionRepository
                .findDocumentVersionByDocument_DocumentIdAndIsActiveIsTrue(version.getDocument().getDocumentId())
                .ifPresent(current -> {
                    current.setActive(false);
                    saveVersion(current);
                });
        version.setActive(true);
        saveVersion(version);

    }

    public void rollbackToPreviousVersion(DocumentVersion version) {
        version.setActive(false);
        saveVersion(version);

        DocumentVersion parent = version.getParentVersion();
        if (parent != null) {
            parent.setActive(true);
            saveVersion(parent);
        }
    }

    public DocumentVersion findLatestApprovedVersionByDocumentId(UUID documentId) {
        return documentVersionRepository.findTopByDocument_DocumentIdAndStatusOrderByCreatedAtDesc(documentId, VersionStatus.APPROVED)
                .orElseThrow(() -> new ApprovedVersionNotFoundException("No approved version found for this document"));
    }

    public Optional<DocumentVersion> findDocumentVersionByDocumentIdAndIsActiveIsTrue(UUID documentId) {
        return documentVersionRepository.findDocumentVersionByDocument_DocumentIdAndIsActiveIsTrue(documentId);
    }

    public DocumentVersion findById(UUID versionId) {
        return documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new VersionNotFoundException("Version not found"));
    }
}
