package main.services;

import jakarta.transaction.Transactional;
import main.entities.*;
import main.repositories.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void createLogForUser(User user, String action, User targetUser, String details) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .targetUser(targetUser)
                .details(details)
                .performedAt(LocalDateTime.now())
                .build();
        saveLog(log);
    }

    public void createLogForDocument(User user, String action, Document document, DocumentVersion version,
                                     DocumentFile file, String details) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .document(document)
                .version(version)
                .file(file)
                .details(details)
                .performedAt(LocalDateTime.now())
                .build();
        saveLog(log);
    }

    public void createLogForDocumentFile(User user, String action, Document document, DocumentVersion documentVersion, DocumentFile file, String details) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .document(document)
                .version(documentVersion)
                .file(file)
                .details(details)
                .performedAt(LocalDateTime.now())
                .build();
        saveLog(log);
    }

    public void createLogForDocumentVersion(User reviewer, String action, Document document, DocumentVersion documentVersion, String details) {
        AuditLog log = AuditLog.builder()
                .user(reviewer)
                .action(action)
                .document(document)
                .version(documentVersion)
                .details(details)
                .performedAt(LocalDateTime.now())
                .build();
        saveLog(log);
    }

    private void saveLog(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
