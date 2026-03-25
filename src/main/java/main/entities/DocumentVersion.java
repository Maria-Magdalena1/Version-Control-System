package main.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_versions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_major", nullable = false)
    private Integer versionMajor;

    @Column(name = "version_minor", nullable = false)
    private Integer versionMinor;

    @Column(name = "version_patch", nullable = false)
    private Integer versionPatch;

    @Column(name = "version_number", nullable = false, length = 20)
    private String versionNumber;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private VersionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_version_id")
    private DocumentVersion parentVersion;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
