package main.services;

import jakarta.transaction.Transactional;
import main.entities.*;
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
            throw new RuntimeException("Only pending versions can be reviewed");
        }

        Approval approval = createApproval(isApproved, comment, reviewer, version);
        saveApproval(approval);
        //lastversion da go napravq false ako se odobri
        if (isApproved) {
            approveVersion(version);
            version.getDocument().setUpdatedAt(LocalDateTime.now());
            documentService.saveDocument(version.getDocument());
        } else {
            version.setStatus(VersionStatus.REJECTED);
        }

        documentVersionService.saveVersion(version);

        auditLogService.createLogForDocument(reviewer,isApproved ? "APPROVED VERSION" : "REJECTED VERSION",
                version.getDocument(),version,file,String.format("The document is %s by %s", isApproved ? "approved" : "rejected", reviewer.getUsername()));

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

    private void approveVersion(DocumentVersion version) {
        //dali da si ostane taka ili da napravq zaqvka
       documentVersionService.findDocumentVersionByDocumentIdAndIsActiveIsTrue(version.getDocument().getDocumentId())
                .ifPresent(oldActive -> {
                    oldActive.setActive(false);
                    documentVersionService.saveVersion(oldActive);
                });
        version.setStatus(VersionStatus.APPROVED);
        version.setActive(true);

    }

    private void saveApproval(Approval approval) {
        approvalRepository.save(approval);
    }
}
