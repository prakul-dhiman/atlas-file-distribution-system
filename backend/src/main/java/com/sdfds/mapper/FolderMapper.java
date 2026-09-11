package com.sdfds.mapper;

import com.sdfds.dto.FolderDto;
import com.sdfds.entity.Folder;
import org.springframework.stereotype.Component;

@Component
public class FolderMapper {

    public FolderDto toDto(Folder folder) {
        if (folder == null) {
            return null;
        }

        return FolderDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .userId(folder.getUser() != null ? folder.getUser().getId() : null)
                .isTrashed(folder.getIsTrashed())
                .trashedAt(folder.getTrashedAt())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}
