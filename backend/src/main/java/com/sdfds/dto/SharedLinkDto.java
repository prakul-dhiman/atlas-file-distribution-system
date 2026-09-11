package com.sdfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedLinkDto {

    private Long id;
    private String token;
    private String name;
    private Long fileId;
    private Long folderId;
    private Long userId;
    private Boolean hasPassword;
    private Instant expiresAt;
    private Long accessCount;
    private Long maxAccessCount;
    private Boolean expireAfterFirstDownload;
    private Boolean requireEmailOtp;
    private Boolean isActive;
    private String shareUrl;
    private String qrCodeUrl;
    private Instant lastAccessedAt;
    private Instant createdAt;
    private List<DownloadEventDto> recentDownloads;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadEventDto {
        private String ipAddress;
        private String userAgent;
        private Instant downloadedAt;
    }
}
