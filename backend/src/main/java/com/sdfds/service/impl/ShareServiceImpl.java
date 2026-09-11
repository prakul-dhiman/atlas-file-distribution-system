package com.sdfds.service.impl;

import com.sdfds.dto.CreateSharedLinkRequest;
import com.sdfds.dto.SharedLinkDto;
import com.sdfds.entity.*;
import com.sdfds.mapper.SharedLinkMapper;
import com.sdfds.repository.*;
import com.sdfds.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private static final Logger log = LoggerFactory.getLogger(ShareServiceImpl.class);

    private final SharedLinkRepository sharedLinkRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ShareDownloadEventRepository downloadEventRepository;
    private final FileService fileService;
    private final FolderService folderService;
    private final SharedLinkMapper sharedLinkMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final com.sdfds.util.UserAgentParser userAgentParser;
    private final com.sdfds.util.GeoIpResolver geoIpResolver;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.public-backend-url:http://localhost:8081}")
    private String publicBackendUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Transactional
    public SharedLinkDto createShareLink(User user, CreateSharedLinkRequest request) {
        FileEntity file = null;
        Folder folder = null;

        if (request.getFileId() != null) {
            file = fileRepository.findByIdAndUserAndIsTrashedFalse(request.getFileId(), user)
                    .orElseThrow(() -> new IllegalArgumentException("File not found or trashed"));
        } else if (request.getFolderId() != null) {
            folder = folderRepository.findByIdAndUserAndIsTrashedFalse(request.getFolderId(), user)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found or trashed"));
        } else {
            throw new IllegalArgumentException("Either fileId or folderId must be provided");
        }

        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        Instant expiresAt = null;
        if (!Boolean.TRUE.equals(request.getNeverExpire())) {
            if (request.getCustomExpiresAt() != null) {
                expiresAt = request.getCustomExpiresAt();
            } else {
                int expiryDays = request.getExpiresAfterDays() != null ? request.getExpiresAfterDays() : 7;
                if (expiryDays < 1 || expiryDays > 90) {
                    throw new IllegalArgumentException("Expiry days must be between 1 and 90");
                }
                expiresAt = Instant.now().plusSeconds(expiryDays * 86400L);
            }
        }

        Long maxAccessCount = request.getMaxAccessCount();
        boolean expireAfterFirst = Boolean.TRUE.equals(request.getExpireAfterFirstDownload());
        if (expireAfterFirst) {
            maxAccessCount = 1L;
        }

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String shareUrl = frontendUrl + "/share/" + token;
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
                + URLEncoder.encode(shareUrl, StandardCharsets.UTF_8);

        SharedLink sharedLink = SharedLink.builder()
                .token(token).file(file).folder(folder).user(user)
                .passwordHash(passwordHash).expiresAt(expiresAt)
                .maxAccessCount(maxAccessCount)
                .expireAfterFirstDownload(expireAfterFirst)
                .requireEmailOtp(Boolean.TRUE.equals(request.getRequireEmailOtp()))
                .qrCodeUrl(qrCodeUrl).isActive(true)
                .build();

        SharedLink saved = sharedLinkRepository.save(sharedLink);
        log.info("Created share link ID: {} token: {}", saved.getId(), token);
        return sharedLinkMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedLinkDto getSharedLinkMetadata(String token) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Shared link not found"));
        validateLinkStatus(link);
        return sharedLinkMapper.toDto(link);
    }

    @Override
    @Transactional
    public Object getSharedLinkContents(String token, String password) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Shared link not found"));
        validateLinkStatus(link);
        validatePassword(link, password);
        return link.getFile() != null
                ? fileService.getFileMetadata(link.getUser(), link.getFile().getId())
                : folderService.getFolderContents(link.getUser(), link.getFolder().getId());
    }

    @Override
    public String generateSignedDownloadUrl(String token, Long fileId, String password) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Shared link not found"));
        validateLinkStatus(link);
        validatePassword(link, password);

        if (link.getFile() != null) {
            if (!link.getFile().getId().equals(fileId))
                throw new IllegalArgumentException("Unauthorized file download request");
        } else {
            FileEntity requestedFile = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, link.getUser())
                    .orElseThrow(() -> new IllegalArgumentException("File not found"));
            Folder folder = requestedFile.getFolder();
            boolean found = false;
            while (folder != null) {
                if (folder.getId().equals(link.getFolder().getId())) { found = true; break; }
                folder = folder.getParent();
            }
            if (!found) throw new IllegalArgumentException("File is not within the shared folder hierarchy");
        }

        long expires = Instant.now().plusSeconds(300).toEpochMilli();
        String signature = calculateHmac(token, fileId, expires);
        return publicBackendUrl + "/api/v1/shares/download/" + token
                + "?fileId=" + fileId + "&expires=" + expires + "&signature=" + signature;
    }

    @Override
    public boolean verifySignature(String token, Long fileId, long expires, String signature) {
        if (Instant.now().toEpochMilli() > expires) return false;
        String expected = calculateHmac(token, fileId, expires);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional
    public FileService.FileDownloadResource downloadSharedFile(
            String token, Long fileId, long expires, String signature,
            String ipAddress, String userAgent) {

        if (!verifySignature(token, fileId, expires, signature))
            throw new SecurityException("Invalid or expired download signature");

        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Shared link not found"));
        validateLinkStatus(link);

        link.setAccessCount(link.getAccessCount() + 1);
        link.setLastAccessedAt(Instant.now());
        if (link.getMaxAccessCount() != null && link.getAccessCount() >= link.getMaxAccessCount())
            link.setIsActive(false);
        sharedLinkRepository.save(link);

        FileEntity fileEntity = fileRepository.findById(fileId).orElse(null);
        com.sdfds.util.UserAgentParser.UserAgentInfo uaInfo = userAgentParser.parse(userAgent);
        com.sdfds.util.GeoIpResolver.GeoInfo geoInfo = geoIpResolver.resolve(ipAddress);

        downloadEventRepository.save(ShareDownloadEvent.builder()
                .sharedLink(link)
                .file(fileEntity)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .userName(link.getUser() != null ? link.getUser().getUsername() : "Anonymous")
                .email(link.getUser() != null ? link.getUser().getEmail() : "anonymous@public")
                .country(geoInfo.getCountry())
                .countryCode(geoInfo.getCountryCode())
                .city(geoInfo.getCity())
                .device(uaInfo.getDevice())
                .browser(uaInfo.getBrowser())
                .operatingSystem(uaInfo.getOperatingSystem())
                .fileSizeBytes(fileEntity != null ? fileEntity.getSizeBytes() : 0L)
                .status("SUCCESS")
                .isUnique(true)
                .build());

        try {
            String fileName = link.getFile() != null ? link.getFile().getName()
                    : link.getFolder() != null ? link.getFolder().getName() : "file";
            emailService.sendDownloadNotification(
                    link.getUser().getEmail(),
                    link.getUser().getFirstName() != null ? link.getUser().getFirstName() : link.getUser().getUsername(),
                    fileName, ipAddress);
        } catch (Exception e) {
            log.warn("Download notification failed: {}", e.getMessage());
        }

        return fileService.downloadFile(link.getUser(), fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharedLinkDto> listUserShares(User user) {
        List<SharedLink> shares = sharedLinkRepository.findByUser(user);
        return shares.stream()
                .map(sharedLinkMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeShare(User user, Long shareId) {
        SharedLink link = sharedLinkRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Shared link not found"));
        if (!link.getUser().getId().equals(user.getId()))
            throw new SecurityException("Unauthorized share revocation request");
        // Soft-delete: mark inactive so it still shows in the list with "Revoked" badge
        link.setIsActive(false);
        sharedLinkRepository.save(link);
    }

    private void validateLinkStatus(SharedLink link) {
        if (!link.getIsActive()) throw new IllegalArgumentException("Shared link is inactive or revoked");
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(Instant.now()))
            throw new IllegalArgumentException("Shared link has expired");
        if (link.getFile() != null && Boolean.TRUE.equals(link.getFile().getIsTrashed()))
            throw new IllegalArgumentException("Shared file is no longer available");
        if (link.getFolder() != null && Boolean.TRUE.equals(link.getFolder().getIsTrashed()))
            throw new IllegalArgumentException("Shared folder is no longer available");
    }

    private void validatePassword(SharedLink link, String password) {
        if (link.getPasswordHash() != null
                && (password == null || !passwordEncoder.matches(password, link.getPasswordHash())))
            throw new IllegalArgumentException("Invalid password for shared link");
    }

    private String calculateHmac(String token, Long fileId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(
                            (token + ":" + fileId + ":" + expires).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC signing error", e);
        }
    }
}
