package main.repositories;

import main.entities.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, UUID> {
}
