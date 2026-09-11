package com.sdfds.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "direct_item_shares")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DirectItemShare {

    public enum PermissionLevel {
        VIEWER,
        EDITOR,
        ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWithUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id", nullable = false)
    private User grantedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel permissionLevel = PermissionLevel.VIEWER;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
