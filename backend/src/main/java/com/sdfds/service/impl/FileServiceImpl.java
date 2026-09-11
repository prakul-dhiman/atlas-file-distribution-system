package com.sdfds.service.impl;

import com.sdfds.dto.FileDto;
import com.sdfds.entity.FileEntity;
import com.sdfds.entity.Folder;
import com.sdfds.entity.User;
import com.sdfds.mapper.FileMapper;
import com.sdfds.repository.FileRepository;
import com.sdfds.repository.FolderRepository;
import com.sdfds.repository.UserRepository;
import com.sdfds.service.FileService;
import com.sdfds.service.MalwareScanner;
import com.sdfds.service.UploadPolicy;
import com.sdfds.storage.LocalStorageService;
import com.sdfds.storage.distribution.ChunkDistributionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final LocalStorageService localStorageService;
    private final ChunkDistributionService chunkDistributionService;
    private final FileMapper fileMapper;
    private final UploadPolicy uploadPolicy;
    private final MalwareScanner malwareScanner;

    @Value("${storage.direct-upload-max-bytes:104857600}") // 100 MB
    private long directUploadMaxBytes = 104857600L;

    @Override
    @Transactional
    public FileDto uploadFile(User user, MultipartFile file, Long folderId) {
        log.info("Upload attempt for file: '{}', size: {} bytes, user ID: {}", file.getOriginalFilename(), file.getSize(), user.getId());

        // Phase 5: enforce upload policy (MIME/extension/size) and malware scan
        uploadPolicy.validate(file);
        malwareScanner.scan(file);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        long newUsedStorage = user.getUsedStorageBytes() + file.getSize();
        if (newUsedStorage > user.getStorageQuotaBytes()) {
            throw new IllegalArgumentException("Storage quota exceeded. Available: " +
                    (user.getStorageQuotaBytes() - user.getUsedStorageBytes()) + " bytes, required: " + file.getSize() + " bytes");
        }

        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findByIdAndUserAndIsTrashedFalse(folderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found or trashed"));
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file";
        String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        boolean useChunking = file.getSize() > directUploadMaxBytes;
        int nextVersion = getNextVersion(user, folder, originalFilename);

        FileEntity fileEntity = FileEntity.builder()
                .name(originalFilename)
                .originalName(originalFilename)
                .mimeType(mimeType)
                .sizeBytes(file.getSize())
                .checksumSha256("PENDING")
                .folder(folder)
                .user(user)
                .isChunked(useChunking)
                .isTrashed(false)
                .version(nextVersion)
                .build();

        FileEntity savedEntity = fileRepository.save(fileEntity);

        try (InputStream inputStream = file.getInputStream()) {
            if (useChunking) {
                log.info("File size exceeds {} bytes threshold. Routing to ChunkDistributionService...", directUploadMaxBytes);
                String overallChecksum = chunkDistributionService.splitAndDistribute(savedEntity, inputStream);
                savedEntity.setChecksumSha256(overallChecksum);
            } else {
                LocalStorageService.WriteResult result = localStorageService.saveFile(user.getId(), savedEntity.getId(), inputStream);
                savedEntity.setChecksumSha256(result.checksumSha256());
                savedEntity.setSizeBytes(result.bytesWritten());
            }
            fileRepository.save(savedEntity);
        } catch (IOException e) {
            log.error("Failed to store file ID: {} for user ID: {}", savedEntity.getId(), user.getId(), e);
            throw new RuntimeException("File storage write failure", e);
        }

        user.setUsedStorageBytes(newUsedStorage);
        userRepository.save(user);

        log.info("File uploaded successfully with ID: {}, isChunked: {}, SHA-256: {}",
                savedEntity.getId(), savedEntity.getIsChunked(), savedEntity.getChecksumSha256());

        return fileMapper.toDto(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDto getFileMetadata(User user, Long fileId) {
        FileEntity fileEntity = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));
        return fileMapper.toDto(fileEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadResource downloadFile(User user, Long fileId) {
        log.info("Download requested for file ID: {} by user ID: {}", fileId, user.getId());

        FileEntity fileEntity = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));

        try {
            Resource resource;
            if (Boolean.TRUE.equals(fileEntity.getIsChunked())) {
                log.info("Assembling chunked file download for file ID: {}", fileId);
                resource = chunkDistributionService.assembleFileStream(fileEntity);
            } else {
                resource = localStorageService.loadFileAsResource(user.getId(), fileEntity.getId());
            }
            return new FileDownloadResource(resource, fileEntity.getName(), fileEntity.getMimeType(), fileEntity.getSizeBytes());
        } catch (IOException e) {
            log.error("Error reading file resource for file ID: {}", fileId, e);
            throw new RuntimeException("Could not read file for download", e);
        }
    }

    @Override
    @Transactional
    public FileDto renameFile(User user, Long fileId, String newName) {
        log.info("Renaming file ID: {} to '{}' for user ID: {}", fileId, newName, user.getId());

        FileEntity fileEntity = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (fileEntity.getName().equals(newName)) {
            return fileMapper.toDto(fileEntity);
        }

        fileEntity.setName(newName);
        FileEntity updated = fileRepository.save(fileEntity);
        return fileMapper.toDto(updated);
    }

    @Override
    @Transactional
    public FileDto moveFile(User user, Long fileId, Long targetFolderId) {
        log.info("Moving file ID: {} to folder ID: {} for user ID: {}", fileId, targetFolderId, user.getId());

        FileEntity fileEntity = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        Folder targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepository.findByIdAndUserAndIsTrashedFalse(targetFolderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Target folder not found"));
        }

        fileEntity.setFolder(targetFolder);
        FileEntity updated = fileRepository.save(fileEntity);
        return fileMapper.toDto(updated);
    }

    @Override
    @Transactional
    public FileDto copyFile(User user, Long fileId, Long targetFolderId) {
        log.info("Copying file ID: {} to target folder ID: {} for user ID: {}", fileId, targetFolderId, user.getId());

        FileEntity sourceFile = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("Source file not found"));

        long newUsedStorage = user.getUsedStorageBytes() + sourceFile.getSizeBytes();
        if (newUsedStorage > user.getStorageQuotaBytes()) {
            throw new IllegalArgumentException("Storage quota exceeded for file copy");
        }

        Folder targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepository.findByIdAndUserAndIsTrashedFalse(targetFolderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Target folder not found"));
        }

        FileEntity newFile = FileEntity.builder()
                .name("Copy of " + sourceFile.getName())
                .originalName(sourceFile.getOriginalName())
                .mimeType(sourceFile.getMimeType())
                .sizeBytes(sourceFile.getSizeBytes())
                .checksumSha256(sourceFile.getChecksumSha256())
                .folder(targetFolder)
                .user(user)
                .isChunked(sourceFile.getIsChunked())
                .isTrashed(false)
                .version(1)
                .build();

        FileEntity savedCopy = fileRepository.save(newFile);

        try {
            if (Boolean.TRUE.equals(sourceFile.getIsChunked())) {
                // Copy chunks for chunked files
                Resource chunkResource = chunkDistributionService.assembleFileStream(sourceFile);
                chunkDistributionService.splitAndDistribute(savedCopy, chunkResource.getInputStream());
            } else {
                localStorageService.copyFile(user.getId(), sourceFile.getId(), savedCopy.getId());
            }
        } catch (IOException e) {
            log.error("Failed to copy physical file ID {} to ID {}", sourceFile.getId(), savedCopy.getId(), e);
            throw new RuntimeException("File copy failed", e);
        }

        user.setUsedStorageBytes(newUsedStorage);
        userRepository.save(user);

        log.info("File copied successfully with new ID: {}", savedCopy.getId());
        return fileMapper.toDto(savedCopy);
    }

    @Override
    @Transactional
    public void deleteFile(User user, Long fileId) {
        log.info("Soft-deleting file ID: {} for user ID: {}", fileId, user.getId());

        FileEntity fileEntity = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        fileEntity.setIsTrashed(true);
        fileEntity.setTrashedAt(Instant.now());
        fileRepository.save(fileEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDto> getFileVersionHistory(User user, Long fileId) {
        FileEntity fileEntity = fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));

        return getVersionSiblings(user, fileEntity.getFolder(), fileEntity.getName()).stream()
                .sorted(Comparator.comparing(FileEntity::getVersion).reversed())
                .map(fileMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FileDto restoreFileVersion(User user, Long fileId, Integer targetVersion) {
        if (targetVersion == null || targetVersion < 1) {
            throw new IllegalArgumentException("Target version must be a positive number");
        }

        FileEntity currentFile = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));

        FileEntity sourceVersion = getVersionSiblings(user, currentFile.getFolder(), currentFile.getName()).stream()
                .filter(file -> targetVersion.equals(file.getVersion()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Version " + targetVersion + " not found for file ID: " + fileId));

        long newUsedStorage = user.getUsedStorageBytes() + sourceVersion.getSizeBytes();
        if (newUsedStorage > user.getStorageQuotaBytes()) {
            throw new IllegalArgumentException("Storage quota exceeded for file version restore");
        }

        int nextVersion = getNextVersion(user, currentFile.getFolder(), currentFile.getName());
        FileEntity restoredVersion = FileEntity.builder()
                .name(currentFile.getName())
                .originalName(sourceVersion.getOriginalName())
                .mimeType(sourceVersion.getMimeType())
                .sizeBytes(sourceVersion.getSizeBytes())
                .checksumSha256(sourceVersion.getChecksumSha256())
                .folder(currentFile.getFolder())
                .user(user)
                .isChunked(sourceVersion.getIsChunked())
                .isTrashed(false)
                .version(nextVersion)
                .build();

        FileEntity savedRestore = fileRepository.save(restoredVersion);

        try {
            if (Boolean.TRUE.equals(sourceVersion.getIsChunked())) {
                Resource chunkResource = chunkDistributionService.assembleFileStream(sourceVersion);
                String checksum = chunkDistributionService.splitAndDistribute(savedRestore, chunkResource.getInputStream());
                savedRestore.setChecksumSha256(checksum);
                savedRestore.setSizeBytes(sourceVersion.getSizeBytes());
            } else {
                localStorageService.copyFile(user.getId(), sourceVersion.getId(), savedRestore.getId());
            }
            fileRepository.save(savedRestore);
        } catch (IOException e) {
            log.error("Failed to restore file ID {} from version {}", fileId, targetVersion, e);
            throw new RuntimeException("File version restore failed", e);
        }

        user.setUsedStorageBytes(newUsedStorage);
        userRepository.save(user);

        return fileMapper.toDto(savedRestore);
    }

    @Override
    @Transactional
    public FileDto toggleStar(User user, Long fileId) {
        FileEntity fileEntity = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));
        fileEntity.setIsStarred(!Boolean.TRUE.equals(fileEntity.getIsStarred()));
        FileEntity saved = fileRepository.save(fileEntity);
        log.info("File ID: {} star status toggled to: {}", fileId, saved.getIsStarred());
        return fileMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDto> getStarredFiles(User user) {
        return fileRepository.findByUserAndIsStarredTrueAndIsTrashedFalse(user).stream()
                .map(fileMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadFilesAsZip(User user, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new IllegalArgumentException("No file IDs provided for zip download");
        }

        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {

            for (Long fileId : fileIds) {
                try {
                    FileDownloadResource dl = downloadFile(user, fileId);
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(dl.filename());
                    zos.putNextEntry(entry);
                    try (InputStream is = dl.resource().getInputStream()) {
                        is.transferTo(zos);
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("Skipping file ID {} in zip download due to error: {}", fileId, e.getMessage());
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate zip file download for user ID: {}", user.getId(), e);
            throw new RuntimeException("Zip download failed", e);
        }
    }

    private int getNextVersion(User user, Folder folder, String filename) {
        return getVersionSiblings(user, folder, filename).stream()
                .map(FileEntity::getVersion)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private List<FileEntity> getVersionSiblings(User user, Folder folder, String filename) {
        return folder == null
                ? fileRepository.findByUserAndNameAndFolderIsNullOrderByVersionDesc(user, filename)
                : fileRepository.findByUserAndNameAndFolderOrderByVersionDesc(user, filename, folder);
    }
}
