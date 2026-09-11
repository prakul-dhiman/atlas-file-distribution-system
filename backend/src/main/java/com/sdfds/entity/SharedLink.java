package com.sdfds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "shared_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "access_count", nullable = false)
    @Builder.Default
    private Long accessCount = 0L;

    @Column(name = "max_access_count")
    private Long maxAccessCount;

    @Column(name = "expire_after_first_download")
    @Builder.Default
    private Boolean expireAfterFirstDownload = false;

    @Column(name = "require_email_otp")
    @Builder.Default
    private Boolean requireEmailOtp = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
