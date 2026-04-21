package main.repositories;

import main.entities.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {
    @Query("""
                SELECT df FROM DocumentFile df
                JOIN FETCH df.uploadedBy
                WHERE df.document.documentId = :documentId
                AND df.isDeleted = false
            """)
    List<DocumentFile> findFilesByDocumentWithUser(UUID documentId);

    Optional<DocumentFile> findByFileIdAndDocument_DocumentIdAndIsDeletedFalse(
            UUID fileId,
            UUID documentId
    );
}
