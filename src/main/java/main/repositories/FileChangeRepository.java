package main.repositories;

import main.entities.FileChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileChangeRepository extends JpaRepository<FileChange, UUID> {
}
