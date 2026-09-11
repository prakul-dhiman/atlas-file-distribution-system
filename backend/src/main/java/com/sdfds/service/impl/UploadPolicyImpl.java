package com.sdfds.service.impl;

import com.sdfds.service.UploadPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Service
public class UploadPolicyImpl implements UploadPolicy {

    private static final Logger log = LoggerFactory.getLogger(UploadPolicyImpl.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "text/markdown",
            "text/html",
            "application/json",
            "application/xml",
            "application/rtf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml",
            "image/bmp",
            "image/tiff",
            "audio/mpeg",
            "audio/wav",
            "audio/ogg",
            "audio/mp4",
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "application/zip",
            "application/gzip",
            "application/x-tar",
            "application/x-7z-compressed",
            "application/x-rar-compressed",
            "text/javascript",
            "application/javascript",
            "text/css",
            "application/typescript",
            "text/x-java-source",
            "text/x-python",
            "text/x-c",
            "text/x-c++",
            "text/x-shellscript",
            "application/x-yaml",
            "application/toml",
            "application/octet-stream"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "scr", "ps1", "vbs", "js", "jse",
            "msi", "msp", "jar", "sh", "dll", "sys", "drv", "cpl", "ocx",
            "hta", "wsf", "wsh", "lnk", "app", "gadget", "msc", "reg"
    );

    @Value("${storage.max-file-size-bytes:5368709120}")
    private long maxFileSizeBytes;

    @Override
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File exceeds maximum allowed size of " + maxFileSizeBytes + " bytes");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = getExtension(originalFilename);
            if (BLOCKED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException("File extension '" + ext + "' is not allowed");
            }
        }

        String mimeType = file.getContentType();
        if (mimeType != null && !mimeType.isBlank()
                && !ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase(Locale.ROOT))
                && !mimeType.startsWith("multipart/")) {
            throw new IllegalArgumentException("File type '" + mimeType + "' is not allowed");
        }

        log.debug("Upload policy passed for file: {}", originalFilename);
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}