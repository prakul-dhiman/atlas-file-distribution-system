package com.sdfds.mapper;

import com.sdfds.dto.FileDto;
import com.sdfds.entity.FileEntity;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

    public FileDto toDto(FileEntity fileEntity) {
        if (fileEntity == null) {
            return null;
        }

        return FileDto.builder()
                .id(fileEntity.getId())
                .name(fileEntity.getName())
                .originalName(fileEntity.getOriginalName())
                .mimeType(fileEntity.getMimeType())
                .sizeBytes(fileEntity.getSizeBytes())
                .checksumSha256(fileEntity.getChecksumSha256())
                .folderId(fileEntity.getFolder() != null ? fileEntity.getFolder().getId() : null)
                .userId(fileEntity.getUser() != null ? fileEntity.getUser().getId() : null)
                .isChunked(fileEntity.getIsChunked())
                .isTrashed(fileEntity.getIsTrashed())
                .trashedAt(fileEntity.getTrashedAt())
                .version(fileEntity.getVersion())
                .createdAt(fileEntity.getCreatedAt())
                .updatedAt(fileEntity.getUpdatedAt())
                .build();
    }
}
