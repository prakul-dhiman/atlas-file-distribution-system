package com.sdfds.service.impl;

import com.sdfds.dto.CreateDirectShareRequest;
import com.sdfds.dto.DirectShareDto;
import com.sdfds.entity.*;
import com.sdfds.entity.DirectItemShare.PermissionLevel;
import com.sdfds.repository.*;
import com.sdfds.service.DirectShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectShareServiceImpl implements DirectShareService {

    private final DirectItemShareRepository directItemShareRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;

    @Override
    @Transactional
    public DirectShareDto createDirectShare(User grantor, CreateDirectShareRequest request) {
        if (request.getFileId() == null && request.getFolderId() == null) {
            throw new IllegalArgumentException("Either fileId or folderId must be provided");
        }

        // Resolve recipient user by username or email
        User recipient = userRepository.findByUsernameOrEmail(
                request.getRecipientIdentifier(), request.getRecipientIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getRecipientIdentifier()));

        if (recipient.getId().equals(grantor.getId())) {
            throw new IllegalArgumentException("Cannot share an item with yourself");
        }

        DirectItemShare.DirectItemShareBuilder builder = DirectItemShare.builder()
                .grantedByUser(grantor)
                .sharedWithUser(recipient)
                .permissionLevel(request.getPermissionLevel() != null
                        ? request.getPermissionLevel() : PermissionLevel.VIEWER);

        if (request.getFileId() != null) {
            FileEntity file = fileRepository.findByIdAndUserAndIsTrashedFalse(request.getFileId(), grantor)
                    .orElseThrow(() -> new IllegalArgumentException("File not found or you don't own it"));
            // Update if exists
            directItemShareRepository.findBySharedWithUserAndFile(recipient, file)
                    .ifPresent(existing -> directItemShareRepository.delete(existing));
            builder.file(file);
        } else {
            Folder folder = folderRepository.findByIdAndUserAndIsTrashedFalse(request.getFolderId(), grantor)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found or you don't own it"));
            directItemShareRepository.findBySharedWithUserAndFolder(recipient, folder)
                    .ifPresent(existing -> directItemShareRepository.delete(existing));
            builder.folder(folder);
        }

        DirectItemShare saved = directItemShareRepository.save(builder.build());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectShareDto> getSharedWithMe(User user) {
        return directItemShareRepository.findBySharedWithUser(user).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectShareDto> getItemCollaborators(User user, Long fileId, Long folderId) {
        if (fileId != null) {
            FileEntity file = fileRepository.findByIdAndUserAndIsTrashedFalse(fileId, user)
                    .orElseThrow(() -> new IllegalArgumentException("File not found"));
            return directItemShareRepository.findByFile(file).stream()
                    .map(this::toDto).collect(Collectors.toList());
        } else if (folderId != null) {
            Folder folder = folderRepository.findByIdAndUserAndIsTrashedFalse(folderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
            return directItemShareRepository.findByFolder(folder).stream()
                    .map(this::toDto).collect(Collectors.toList());
        }
        throw new IllegalArgumentException("Either fileId or folderId must be provided");
    }

    @Override
    @Transactional
    public DirectShareDto updatePermission(User user, Long shareId, PermissionLevel level) {
        DirectItemShare share = directItemShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));
        if (!share.getGrantedByUser().getId().equals(user.getId())) {
            throw new SecurityException("You don't have permission to modify this share");
        }
        share.setPermissionLevel(level);
        share.setUpdatedAt(Instant.now());
        return toDto(directItemShareRepository.save(share));
    }

    @Override
    @Transactional
    public void revokeShare(User user, Long shareId) {
        DirectItemShare share = directItemShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));
        if (!share.getGrantedByUser().getId().equals(user.getId())) {
            throw new SecurityException("You don't have permission to revoke this share");
        }
        directItemShareRepository.delete(share);
    }

    private DirectShareDto toDto(DirectItemShare share) {
        return DirectShareDto.builder()
                .id(share.getId())
                .fileId(share.getFile() != null ? share.getFile().getId() : null)
                .fileName(share.getFile() != null ? share.getFile().getName() : null)
                .folderId(share.getFolder() != null ? share.getFolder().getId() : null)
                .folderName(share.getFolder() != null ? share.getFolder().getName() : null)
                .sharedWithUserId(share.getSharedWithUser().getId())
                .sharedWithUsername(share.getSharedWithUser().getUsername())
                .sharedWithEmail(share.getSharedWithUser().getEmail())
                .grantedByUserId(share.getGrantedByUser().getId())
                .grantedByUsername(share.getGrantedByUser().getUsername())
                .permissionLevel(share.getPermissionLevel())
                .createdAt(share.getCreatedAt())
                .build();
    }
}
