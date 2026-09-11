package com.sdfds.dto;

import com.sdfds.entity.DirectItemShare.PermissionLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDirectShareRequest {

    private Long fileId;
    private Long folderId;

    @NotBlank(message = "Recipient username or email is required")
    private String recipientIdentifier;

    private PermissionLevel permissionLevel = PermissionLevel.VIEWER;
}
