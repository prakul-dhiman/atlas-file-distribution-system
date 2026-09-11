package com.sdfds.service.impl;

import com.sdfds.dto.FileDto;
import com.sdfds.dto.FolderDto;
import com.sdfds.dto.RecycleBinResponse;
import com.sdfds.entity.FileEntity;
import com.sdfds.entity.Folder;
import com.sdfds.entity.User;
import com.sdfds.mapper.FileMapper;
import com.sdfds.mapper.FolderMapper;
import com.sdfds.repository.FileRepository;
import com.sdfds.repository.FolderRepository;
import com.sdfds.repository.UserRepository;
import com.sdfds.service.RecycleBinService;
import com.sdfds.storage.LocalStorageService;
import com.sdfds.storage.distribution.ChunkDistributionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinServiceImpl.class);
    private static final int AUTO_PURGE_DAYS = 30;

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final LocalStorageService localStorageService;
    private final ChunkDistributionService chunkDistributionService;
    private final FileMapper fileMapper;
    private final FolderMapper folderMapper;

    @Override
    @Transactional(readOnly = true)
    public RecycleBinResponse listTrashedItems(User user) {
        log.info("Listing trashed items for user ID: {}", user.getId());

        List<FolderDto> trashedFolders = folderRepository.findByUserAndIsTrashedTrue(user)
                .stream()
                .map(folderMapper::toDto)
                .collect(Collectors.toList());

        List<FileDto> trashedFiles = fileRepository.findByUserAndIsTrashedTrue(user)
                .stream()
                .map(fileMapper::toDto)
                .collect(Collectors.toList());

        return RecycleBinResponse.builder()
                .trashedFolders(trashedFolders)
                .trashedFiles(trashedFiles)
                .totalItems(trashedFolders.size() + trashedFiles.size())
                .build();
    }

    @Override
    @Transactional
    public FolderDto restoreFolder(User user, Long folderId) {
        log.info("Restoring folder ID: {} for user ID: {}", folderId, user.getId());

        Folder folder = folderRepository.findByIdAndUserAndIsTrashedTrue(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Trashed folder not found with ID: " + folderId));

        // If parent is still trashed, restore to root
        if (folder.getParent() != null && Boolean.TRUE.equals(folder.getParent().getIsTrashed())) {
            folder.setParent(null);
        }

        folder.setIsTrashed(false);
        folder.setTrashedAt(null);
        Folder restored = folderRepository.save(folder);

        // Recursively restore all child folders and files
        cascadeRestoreFolder(folder);

        log.info("Folder ID: {} restored successfully", folderId);
        return folderMapper.toDto(restored);
    }

    @Override
    @Transactional
    public FileDto restoreFile(User user, Long fileId) {
        log.info("Restoring file ID: {} for user ID: {}", fileId, user.getId());

        FileEntity file = fileRepository.findByIdAndUserAndIsTrashedTrue(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("Trashed file not found with ID: " + fileId));

        // If parent folder is still trashed, restore file to root
        if (file.getFolder() != null && Boolean.TRUE.equals(file.getFolder().getIsTrashed())) {
            file.setFolder(null);
        }

        file.setIsTrashed(false);
        file.setTrashedAt(null);
        FileEntity restored = fileRepository.save(file);

        log.info("File ID: {} restored successfully", fileId);
        return fileMapper.toDto(restored);
    }

    @Override
    @Transactional
    public void purgeFolder(User user, Long folderId) {
        log.info("Permanently purging folder ID: {} for user ID: {}", folderId, user.getId());

        Folder folder = folderRepository.findByIdAndUserAndIsTrashedTrue(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Trashed folder not found with ID: " + folderId));

        permanentlyDeleteFolder(folder);
        log.info("Folder ID: {} permanently purged", folderId);
    }

    @Override
    @Transactional
    public void purgeFile(User user, Long fileId) {
        log.info("Permanently purging file ID: {} for user ID: {}", fileId, user.getId());

        FileEntity file = fileRepository.findByIdAndUserAndIsTrashedTrue(fileId, user)
                .orElseThrow(() -> new IllegalArgumentException("Trashed file not found with ID: " + fileId));

        permanentlyDeleteFile(file);
        log.info("File ID: {} permanently purged", fileId);
    }

    @Override
    @Transactional
    public void emptyTrash(User user) {
        log.info("Emptying trash for user ID: {}", user.getId());

        List<FileEntity> trashedFiles = fileRepository.findByUserAndIsTrashedTrue(user);
        for (FileEntity file : trashedFiles) {
            permanentlyDeleteFile(file);
        }

        List<Folder> trashedFolders = folderRepository.findByUserAndIsTrashedTrue(user);
        for (Folder folder : trashedFolders) {
            permanentlyDeleteFolder(folder);
        }

        log.info("Trash emptied for user ID: {}. Purged {} files and {} folders.",
                user.getId(), trashedFiles.size(), trashedFolders.size());
    }

    @Override
    @Transactional
    public void autoCleanup() {
        Instant cutoff = Instant.now().minus(AUTO_PURGE_DAYS, ChronoUnit.DAYS);
        log.info("Running auto-cleanup: purging items trashed before {}", cutoff);

        List<FileEntity> trashedFiles = fileRepository.findByIsTrashedTrueAndTrashedAtBefore(cutoff);
        for (FileEntity file : trashedFiles) {
            permanentlyDeleteFile(file);
        }

        List<Folder> trashedFolders = folderRepository.findByIsTrashedTrueAndTrashedAtBefore(cutoff);
        for (Folder folder : trashedFolders) {
            permanentlyDeleteFolder(folder);
        }

        log.info("Auto-cleanup completed: purged {} files and {} folders", trashedFiles.size(), trashedFolders.size());
    }

    // --- Helper methods ---

    private void cascadeRestoreFolder(Folder folder) {
        // Restore child files
        List<FileEntity> childFiles = fileRepository.findByFolder(folder);
        for (FileEntity file : childFiles) {
            if (Boolean.TRUE.equals(file.getIsTrashed())) {
                file.setIsTrashed(false);
                file.setTrashedAt(null);
                fileRepository.save(file);
            }
        }

        // Recursively restore child folders
        List<Folder> childFolders = folderRepository.findByParent(folder);
        for (Folder child : childFolders) {
            if (Boolean.TRUE.equals(child.getIsTrashed())) {
                child.setIsTrashed(false);
                child.setTrashedAt(null);
                folderRepository.save(child);
                cascadeRestoreFolder(child);
            }
        }
    }

    private void permanentlyDeleteFile(FileEntity file) {
        Long userId = file.getUser().getId();
        long fileSize = file.getSizeBytes();

        // Delete physical file from storage
        if (Boolean.TRUE.equals(file.getIsChunked())) {
            chunkDistributionService.deleteChunksForFile(file);
        } else {
            localStorageService.deleteFile(userId, file.getId());
        }

        // Reduce user's used storage
        User user = file.getUser();
        user.setUsedStorageBytes(Math.max(0, user.getUsedStorageBytes() - fileSize));
        userRepository.save(user);

        fileRepository.delete(file);
    }

    private void permanentlyDeleteFolder(Folder folder) {
        // First, recursively delete all child files and folders
        List<FileEntity> childFiles = fileRepository.findByFolder(folder);
        for (FileEntity file : childFiles) {
            permanentlyDeleteFile(file);
        }

        List<Folder> childFolders = folderRepository.findByParent(folder);
        for (Folder child : childFolders) {
            permanentlyDeleteFolder(child);
        }

        folderRepository.delete(folder);
    }
}
