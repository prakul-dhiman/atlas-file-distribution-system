package com.sdfds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "share_download_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareDownloadEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_link_id", nullable = false)
    private SharedLink sharedLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "device", length = 50)
    private String device;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "operating_system", length = 50)
    private String operatingSystem;

    @Column(name = "download_speed_bps")
    private Long downloadSpeedBps;

    @Column(name = "download_duration_ms")
    private Long downloadDurationMs;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "referer", length = 512)
    private String referer;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "is_unique")
    @Builder.Default
    private Boolean isUnique = true;

    @Column(name = "downloaded_at", updatable = false)
    @Builder.Default
    private Instant downloadedAt = Instant.now();
}
