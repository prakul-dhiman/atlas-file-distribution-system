package com.sdfds.service.impl;

import com.sdfds.dto.*;
import com.sdfds.entity.FileEntity;
import com.sdfds.entity.Folder;
import com.sdfds.entity.User;
import com.sdfds.mapper.FileMapper;
import com.sdfds.mapper.FolderMapper;
import com.sdfds.repository.FileRepository;
import com.sdfds.repository.FolderRepository;
import com.sdfds.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private static final Logger log = LoggerFactory.getLogger(FolderServiceImpl.class);

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final FolderMapper folderMapper;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public FolderDto createFolder(User user, CreateFolderRequest request) {
        log.info("Creating folder '{}' for user ID: {}, parent ID: {}", request.getName(), user.getId(), request.getParentId());

        Folder parentFolder = null;
        if (request.getParentId() != null) {
            parentFolder = folderRepository.findByIdAndUserAndIsTrashedFalse(request.getParentId(), user)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found or trashed"));
        }

        boolean exists = parentFolder == null
                ? folderRepository.existsByUserAndNameAndParentIsNullAndIsTrashedFalse(user, request.getName())
                : folderRepository.existsByUserAndNameAndParentAndIsTrashedFalse(user, request.getName(), parentFolder);

        if (exists) {
            throw new IllegalArgumentException("Folder with name '" + request.getName() + "' already exists in target directory");
        }

        Folder folder = Folder.builder()
                .name(request.getName())
                .parent(parentFolder)
                .user(user)
                .isTrashed(false)
                .build();

        Folder savedFolder = folderRepository.save(folder);
        log.info("Folder created successfully with ID: {}", savedFolder.getId());
        return folderMapper.toDto(savedFolder);
    }

    @Override
    @Transactional(readOnly = true)
    public FolderContentResponse getFolderContents(User user, Long folderId) {
        Folder currentFolder = null;
        if (folderId != null) {
            currentFolder = folderRepository.findByIdAndUserAndIsTrashedFalse(folderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found with ID: " + folderId));
        }

        List<Folder> subfolders = currentFolder == null
                ? folderRepository.findByUserAndParentIsNullAndIsTrashedFalse(user)
                : folderRepository.findByUserAndParentAndIsTrashedFalse(user, currentFolder);

        List<FileEntity> files = currentFolder == null
                ? fileRepository.findLatestActiveRootFiles(user)
                : fileRepository.findLatestActiveFilesInFolder(user, currentFolder);

        return FolderContentResponse.builder()
                .currentFolder(folderMapper.toDto(currentFolder))
                .subfolders(subfolders.stream().map(folderMapper::toDto).collect(Collectors.toList()))
                .files(files.stream().map(fileMapper::toDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FolderDto> getSubfolders(User user, Long parentFolderId) {
        Folder parentFolder = null;
        if (parentFolderId != null) {
            parentFolder = folderRepository.findByIdAndUserAndIsTrashedFalse(parentFolderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        }

        List<Folder> subfolders = parentFolder == null
                ? folderRepository.findByUserAndParentIsNullAndIsTrashedFalse(user)
                : folderRepository.findByUserAndParentAndIsTrashedFalse(user, parentFolder);

        return subfolders.stream().map(folderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FolderDto renameFolder(User user, Long folderId, String newName) {
        log.info("Renaming folder ID: {} to '{}' for user ID: {}", folderId, newName, user.getId());

        Folder folder = folderRepository.findByIdAndUserAndIsTrashedFalse(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (folder.getName().equals(newName)) {
            return folderMapper.toDto(folder);
        }

        boolean exists = folder.getParent() == null
                ? folderRepository.existsByUserAndNameAndParentIsNullAndIsTrashedFalse(user, newName)
                : folderRepository.existsByUserAndNameAndParentAndIsTrashedFalse(user, newName, folder.getParent());

        if (exists) {
            throw new IllegalArgumentException("Folder name '" + newName + "' already exists in current directory");
        }

        folder.setName(newName);
        Folder updated = folderRepository.save(folder);
        return folderMapper.toDto(updated);
    }

    @Override
    @Transactional
    public FolderDto moveFolder(User user, Long folderId, Long targetParentId) {
        log.info("Moving folder ID: {} to target parent ID: {} for user ID: {}", folderId, targetParentId, user.getId());

        Folder folder = folderRepository.findByIdAndUserAndIsTrashedFalse(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (Objects.equals(folderId, targetParentId)) {
            throw new IllegalArgumentException("Cannot move folder into itself");
        }

        Folder targetParent = null;
        if (targetParentId != null) {
            targetParent = folderRepository.findByIdAndUserAndIsTrashedFalse(targetParentId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Target parent folder not found"));

            // Circular reference check
            Folder current = targetParent;
            while (current != null) {
                if (current.getId().equals(folderId)) {
                    throw new IllegalArgumentException("Cannot move folder into one of its own subdirectories");
                }
                current = current.getParent();
            }
        }

        folder.setParent(targetParent);
        Folder updated = folderRepository.save(folder);
        return folderMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteFolder(User user, Long folderId) {
        log.info("Soft-deleting folder ID: {} for user ID: {}", folderId, user.getId());

        Folder folder = folderRepository.findByIdAndUserAndIsTrashedFalse(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        cascadeSoftDeleteFolder(folder, Instant.now());
    }

    private void cascadeSoftDeleteFolder(Folder folder, Instant trashedAt) {
        folder.setIsTrashed(true);
        folder.setTrashedAt(trashedAt);
        folderRepository.save(folder);

        List<Folder> subfolders = folderRepository.findByParent(folder);
        for (Folder subfolder : subfolders) {
            cascadeSoftDeleteFolder(subfolder, trashedAt);
        }

        List<FileEntity> files = fileRepository.findByFolder(folder);
        for (FileEntity file : files) {
            file.setIsTrashed(true);
            file.setTrashedAt(trashedAt);
            fileRepository.save(file);
        }
    }
}
