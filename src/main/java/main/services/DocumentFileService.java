package main.services;

import main.entities.*;
import main.repositories.DocumentFileRepository;
import main.repositories.FileChangeRepository;
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

@Service
public class DocumentFileService {

    private final DocumentFileRepository documentFileRepository;
    private final FileChangeRepository fileChangeRepository;

    @Autowired
    public DocumentFileService(DocumentFileRepository documentFileRepository, FileChangeRepository fileChangeRepository) {
        this.documentFileRepository = documentFileRepository;
        this.fileChangeRepository = fileChangeRepository;
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
                    null, "File uploaded: " + fileName, uploadedBy);

            return documentFile;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(UUID fileId, DocumentVersion version, User deletedBy) {
        DocumentFile file = documentFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (file.isDeleted()) {
            throw new IllegalStateException("File is already deleted");
        }

        file.setDeleted(true);
        documentFileRepository.save(file);

        saveFileChange(version, file, ChangeType.DELETED,
                null, "File deleted: " + file.getFileName(), deletedBy);
    }

    public List<DocumentFile> getFilesByDocument(UUID documentId) {
        return documentFileRepository.findByDocument_DocumentIdAndIsDeletedFalse(documentId);
    }

    private void saveFileChange(DocumentVersion version, DocumentFile file,
                                ChangeType changeType, String oldFileName,
                                String changeSummary, User changedBy) {
        FileChange fileChange = new FileChange();
        fileChange.setDocumentVersion(version);
        fileChange.setFile(file);
        fileChange.setChangeType(changeType);
        fileChange.setOldFileName(oldFileName);
        fileChange.setChangeSummary(changeSummary);
        fileChange.setChangedBy(changedBy);
        fileChange.setChangedAt(LocalDateTime.now());
        fileChangeRepository.save(fileChange);
    }

}
