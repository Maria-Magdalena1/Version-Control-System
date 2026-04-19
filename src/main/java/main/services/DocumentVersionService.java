package main.services;

import jakarta.transaction.Transactional;
import main.entities.Document;
import main.entities.DocumentVersion;
import main.entities.VersionStatus;
import main.exceptions.ApprovedVersionNotFoundException;
import main.exceptions.VersionNotFoundException;
import main.repositories.DocumentVersionRepository;
import main.web.VersionComparisonResult;
import org.springframework.beans.factory.annotation.Autowired;
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
        } else {
            throw new IllegalStateException(
                    "Cannot rollback version " + version.getVersionNumber() +
                            " — it is the first version and has no previous version to restore."
                    //log
            );
        }
    }

    public VersionComparisonResult compareVersions(UUID version1, UUID version2) {
        DocumentVersion v1 =findById(version1);
        DocumentVersion v2 =findById(version2);

        List<String> lines1 = Arrays.asList(v1.getContent().split("\n"));
        List<String> lines2 = Arrays.asList(v2.getContent().split("\n"));

        List<String> addedLines =lines2.stream()
                .filter(line->!lines1.contains(line))
                .collect(Collectors.toList());

        List<String> removedLines =lines1.stream()
                .filter(line->!lines2.contains(line))
                .collect(Collectors.toList());

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

    public List<DocumentVersion> getVersionHistory(UUID documentId) {
        return documentVersionRepository.findAllByDocument_DocumentIdOrderByCreatedAtAsc(documentId);
    }

    public List<DocumentVersion> findApprovedVersionsByDocumentId(UUID documentId) {
        return documentVersionRepository
                .findAllByDocument_DocumentIdAndStatusOrderByCreatedAtAsc(documentId, VersionStatus.APPROVED);
    }
}
