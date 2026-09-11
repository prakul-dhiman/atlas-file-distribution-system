package com.sdfds.mapper;

import com.sdfds.dto.SharedLinkDto;
import com.sdfds.entity.ShareDownloadEvent;
import com.sdfds.entity.SharedLink;
import com.sdfds.repository.ShareDownloadEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SharedLinkMapper {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final ShareDownloadEventRepository downloadEventRepository;

    public SharedLinkMapper(ShareDownloadEventRepository downloadEventRepository) {
        this.downloadEventRepository = downloadEventRepository;
    }

    public SharedLinkDto toDto(SharedLink link) {
        if (link == null) return null;

        String name = "";
        if (link.getFile() != null) {
            name = link.getFile().getName();
        } else if (link.getFolder() != null) {
            name = link.getFolder().getName();
        }

        List<SharedLinkDto.DownloadEventDto> recentDownloads = downloadEventRepository
                .findBySharedLinkOrderByDownloadedAtDesc(link)
                .stream()
                .limit(10)
                .map(e -> SharedLinkDto.DownloadEventDto.builder()
                        .ipAddress(e.getIpAddress())
                        .userAgent(e.getUserAgent())
                        .downloadedAt(e.getDownloadedAt())
                        .build())
                .collect(Collectors.toList());

        return SharedLinkDto.builder()
                .id(link.getId())
                .token(link.getToken())
                .name(name)
                .fileId(link.getFile() != null ? link.getFile().getId() : null)
                .folderId(link.getFolder() != null ? link.getFolder().getId() : null)
                .userId(link.getUser() != null ? link.getUser().getId() : null)
                .hasPassword(link.getPasswordHash() != null)
                .expiresAt(link.getExpiresAt())
                .accessCount(link.getAccessCount())
                .maxAccessCount(link.getMaxAccessCount())
                .expireAfterFirstDownload(link.getExpireAfterFirstDownload())
                .requireEmailOtp(link.getRequireEmailOtp())
                .isActive(link.getIsActive())
                .shareUrl(frontendUrl + "/share/" + link.getToken())
                .qrCodeUrl(link.getQrCodeUrl())
                .lastAccessedAt(link.getLastAccessedAt())
                .createdAt(link.getCreatedAt())
                .recentDownloads(recentDownloads)
                .build();
    }
}
