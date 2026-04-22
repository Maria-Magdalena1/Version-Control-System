package main.services;

import jakarta.transaction.Transactional;
import main.entities.*;
import main.exceptions.InvalidVersionStatusException;
import main.repositories.ApprovalRepository;
import main.repositories.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final DocumentVersionService documentVersionService;
    private final AuditLogService auditLogService;
    private final DocumentRepository documentRepository;

    @Autowired
    public ApprovalService(ApprovalRepository approvalRepository, DocumentVersionService documentVersionService, AuditLogService auditLogService, DocumentRepository documentRepository) {
        this.approvalRepository = approvalRepository;
        this.documentVersionService = documentVersionService;
        this.auditLogService = auditLogService;
        this.documentRepository = documentRepository;
    }

    @PreAuthorize("hasRole('REVIEWER')")
    public void reviewVersion(UUID versionId, boolean isApproved, String comment, User reviewer) {
        DocumentVersion version = documentVersionService.findById(versionId);

        if (version.getStatus() != VersionStatus.PENDING) {
            throw new InvalidVersionStatusException("Only pending versions can be reviewed");
        }

        Approval approval = createApproval(isApproved, comment, reviewer, version);
        saveApproval(approval);
        if (isApproved) {
            approveVersion(version, reviewer);
        } else {
            rejectVersion(version, reviewer, comment);
        }
    }

    private static Approval createApproval(boolean isApproved, String comment, User reviewer, DocumentVersion version) {
        return Approval.builder()
                .documentVersion(version)
                .reviewedBy(reviewer)
                .decision(isApproved ? Decision.APPROVED : Decision.REJECTED)
                .comment(comment)
                .reviewedAt(LocalDateTime.now())
                .build();
    }

    @PreAuthorize("hasRole('REVIEWER')")
    public void approveVersion(DocumentVersion version, User reviewer) {
        version.setStatus(VersionStatus.APPROVED);
        documentVersionService.activateVersion(version, reviewer);

        version.getDocument().setUpdatedAt(LocalDateTime.now());
        documentRepository.save(version.getDocument());

        auditLogService.createLogForDocument(reviewer, "APPROVE VERSION", version.getDocument(),
                version, null, String.format("Version %s approved by %s.", version.getVersionNumber(), reviewer.getUsername()));
    }

    @PreAuthorize("hasRole('REVIEWER')")
    public void rejectVersion(DocumentVersion version, User reviewer, String reason) {

        version.setStatus(VersionStatus.REJECTED);
        version.setComment(reason);
        documentVersionService.rollbackToPreviousVersion(version, reviewer);

        version.getDocument().setUpdatedAt(LocalDateTime.now());
        documentRepository.save(version.getDocument());

        auditLogService.createLogForDocument(reviewer, "REJECT VERSION", version.getDocument(),
                version, null, String.format("Version %s rejected by %s. Reason: %s", version.getVersionNumber(), reviewer.getUsername(), reason));
    }

    private void saveApproval(Approval approval) {
        approvalRepository.save(approval);
    }
}
