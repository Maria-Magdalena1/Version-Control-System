package main.services;

import jakarta.transaction.Transactional;
import main.entities.*;
import main.repositories.DocumentFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Transactional
@Service
public class DocumentFileService {

    private final DocumentFileRepository documentFileRepository;
    private final FileChangeService fileChangeService;
    private final AuditLogService auditLogService;

    @Autowired
    public DocumentFileService(DocumentFileRepository documentFileRepository, FileChangeService fileChangeService, AuditLogService auditLogService) {
        this.documentFileRepository = documentFileRepository;
        this.fileChangeService = fileChangeService;
        this.auditLogService = auditLogService;
    }

    public DocumentFile uploadFile(String sourcePath, Document document,
                                   DocumentVersion version, User uploadedBy) {

        Path source = Paths.get(sourcePath);

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("File not found: " + sourcePath);
        }

        try {
            String fileName = source.getFileName().toString();
            Path destination = Paths.get("uploads", document.getDocumentId().toString(), fileName);
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            DocumentFile documentFile = DocumentFile.builder()
                    .document(document)
                    .fileName(fileName)
                    .filePath(destination.toString())
                    .fileSizeBytes(Files.size(source))
                    .mimeType(Files.probeContentType(source))
                    .uploadedBy(uploadedBy)
                    .uploadedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            documentFileRepository.save(documentFile);

            saveFileChange(version, documentFile, ChangeType.ADDED,
                    "File uploaded: " + fileName, uploadedBy);

            auditLogService.createLogForDocumentFile(uploadedBy, "UPLOAD DOCUMENT FILE", document,
                    version, documentFile, String.format("Uploaded file with name %s by %s", fileName, uploadedBy.getUsername()));
            return documentFile;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(UUID fileId, DocumentVersion version, User deletedBy) {
        DocumentFile file = documentFileRepository
                .findByFileIdAndDocument_DocumentIdAndIsDeletedFalse(
                        fileId,
                        version.getDocument().getDocumentId()
                )
                .orElseThrow(() -> new IllegalArgumentException("File not found for this document"));

        file.setDeleted(true);
        documentFileRepository.save(file);

        saveFileChange(version, file, ChangeType.DELETED,
                "File deleted: " + file.getFileName(), deletedBy);
        auditLogService.createLogForDocumentFile(deletedBy, "DELETE DOCUMENT FILE", version.getDocument(),
                version, file, String.format("Deleted file by %s", deletedBy.getUsername()));
    }

    public List<DocumentFile> getFilesByDocument(UUID documentId) {
        return documentFileRepository.findFilesByDocumentWithUser(documentId);
    }

    private void saveFileChange(DocumentVersion version, DocumentFile file,
                                ChangeType changeType,
                                String changeSummary, User changedBy) {
        FileChange fileChange = new FileChange();
        fileChange.setDocumentVersion(version);
        fileChange.setFile(file);
        fileChange.setChangeType(changeType);
        fileChange.setChangeSummary(changeSummary);
        fileChange.setChangedBy(changedBy);
        fileChange.setChangedAt(LocalDateTime.now());
        fileChangeService.saveFile(fileChange);
        auditLogService.createLogForDocumentFile(changedBy, "CHANGE DOCUMENT FILE", version.getDocument(),
                version, file, String.format("Changed file by %s", changedBy.getUsername()));
    }

}
