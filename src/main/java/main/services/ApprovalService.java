package main.services;

import jakarta.transaction.Transactional;
import main.entities.*;
import main.exceptions.InvalidVersionStatusException;
import main.repositories.ApprovalRepository;
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
    private final DocumentService documentService;

    //slojila sum go prosto da ne dava greshki zashtoto oshte ne sum dobavila kod za file
    DocumentFile file;

    @Autowired
    public ApprovalService(ApprovalRepository approvalRepository, DocumentVersionService documentVersionService, AuditLogService auditLogService, DocumentService documentService) {
        this.approvalRepository = approvalRepository;
        this.documentVersionService = documentVersionService;
        this.auditLogService = auditLogService;
        this.documentService = documentService;
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
            approveVersion(versionId,reviewer);
        } else {
            rejectVersion(versionId,reviewer,comment);
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
    public void approveVersion(UUID versionId, User reviewer) {
        DocumentVersion version = documentVersionService.findById(versionId);

        if (version.getStatus() != VersionStatus.PENDING) {
            throw new InvalidVersionStatusException("Only pending versions can be approved");
        }

        version.setStatus(VersionStatus.APPROVED);
        documentVersionService.activateVersion(version);

        version.getDocument().setUpdatedAt(LocalDateTime.now());
        documentService.saveDocument(version.getDocument());

        auditLogService.createLogForDocument(reviewer, "APPROVE VERSION", version.getDocument(),
                version, file, String.format("Version %s approved by %s.", version.getVersionNumber(), reviewer));
    }

    @PreAuthorize("hasRole('REVIEWER')")
    public void rejectVersion(UUID versionId, User reviewer, String reason) {
        DocumentVersion version = documentVersionService.findById(versionId);

        if (version.getStatus() != VersionStatus.PENDING) {
            throw new InvalidVersionStatusException("Only pending versions can be rejected");
        }

        version.setStatus(VersionStatus.REJECTED);
        version.setComment(reason);
        documentVersionService.rollbackToPreviousVersion(version);

        version.getDocument().setUpdatedAt(LocalDateTime.now());
        documentService.saveDocument(version.getDocument());

        auditLogService.createLogForDocument(reviewer, "REJECT VERSION", version.getDocument(),
                version, file, String.format("Version %s rejected by %s. Reason: %s", version.getVersionNumber(), reviewer, reason));
    }

    private void saveApproval(Approval approval) {
        approvalRepository.save(approval);
    }
}
