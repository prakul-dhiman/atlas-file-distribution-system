package com.sdfds.storage;

import com.sdfds.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class LocalStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootStoragePath;

    public LocalStorageService(@Value("${storage.local-dir:./storage-data}") String storageDir) {
        this.rootStoragePath = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootStoragePath);
            log.info("Initialized LocalStorageService at path: {}", this.rootStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local storage directory", e);
        }
    }

    public WriteResult saveFile(Long userId, Long fileId, InputStream inputStream) throws IOException {
        Path userDir = rootStoragePath.resolve(String.valueOf(userId));
        Files.createDirectories(userDir);

        Path filePath = userDir.resolve(String.valueOf(fileId));

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }

        long bytesWritten = 0;
        try (DigestInputStream dis = new DigestInputStream(inputStream, digest);
             OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                bytesWritten += bytesRead;
            }
        }

        String checksum = HashUtils.bytesToHex(digest.digest());
        log.info("Saved local file ID: {} for user ID: {}, size: {} bytes, SHA-256: {}", fileId, userId, bytesWritten, checksum);

        return new WriteResult(bytesWritten, checksum);
    }

    public Resource loadFileAsResource(Long userId, Long fileId) throws IOException {
        Path filePath = rootStoragePath.resolve(String.valueOf(userId)).resolve(String.valueOf(fileId));
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException("File not found or not readable at path: " + filePath);
        }
    }

    public boolean deleteFile(Long userId, Long fileId) {
        Path filePath = rootStoragePath.resolve(String.valueOf(userId)).resolve(String.valueOf(fileId));
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            log.info("Deleted physical local file ID: {} for user ID: {} (deleted: {})", fileId, userId, deleted);
            return deleted;
        } catch (IOException e) {
            log.error("Error deleting local file ID: {} for user ID: {}", fileId, userId, e);
            return false;
        }
    }

    public void copyFile(Long userId, Long sourceFileId, Long targetFileId) throws IOException {
        Path sourcePath = rootStoragePath.resolve(String.valueOf(userId)).resolve(String.valueOf(sourceFileId));
        Path targetPath = rootStoragePath.resolve(String.valueOf(userId)).resolve(String.valueOf(targetFileId));
        Files.createDirectories(targetPath.getParent());

        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied physical file from ID {} to ID {} for user ID: {}", sourceFileId, targetFileId, userId);
    }

    public record WriteResult(long bytesWritten, String checksumSha256) {}
}
