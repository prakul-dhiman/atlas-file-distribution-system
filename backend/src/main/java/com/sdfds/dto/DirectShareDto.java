package com.sdfds.dto;

import com.sdfds.entity.DirectItemShare.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectShareDto {

    private Long id;
    private Long fileId;
    private String fileName;
    private Long folderId;
    private String folderName;
    private Long sharedWithUserId;
    private String sharedWithUsername;
    private String sharedWithEmail;
    private Long grantedByUserId;
    private String grantedByUsername;
    private PermissionLevel permissionLevel;
    private Instant createdAt;
}
