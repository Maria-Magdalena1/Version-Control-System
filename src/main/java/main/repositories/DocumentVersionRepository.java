package main.repositories;

import main.entities.DocumentVersion;
import main.entities.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    Optional<DocumentVersion> findTopByDocument_DocumentIdAndStatusOrderByCreatedAtDesc(UUID documentId, VersionStatus versionStatus);

    Optional<DocumentVersion> findDocumentVersionByDocument_DocumentIdAndIsActiveIsTrue(UUID documentId);

}