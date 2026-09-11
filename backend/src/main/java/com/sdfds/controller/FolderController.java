package com.sdfds.controller;

import com.sdfds.dto.*;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.FolderService;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
@Tag(name = "Folders", description = "Folder creation, navigation, renaming, moving, and soft deletion")
@SecurityRequirement(name = "Bearer Authentication")
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create folder", description = "Creates a new folder under root or parent directory")
    public ResponseEntity<ApiResponse<FolderDto>> createFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateFolderRequest request
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FolderDto folder = folderService.createFolder(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(folder, "Folder created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get folder contents", description = "Retrieves subfolders and files for a parent folder (or root if parentId omitted)")
    public ResponseEntity<ApiResponse<FolderContentResponse>> getFolderContents(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Long parentId
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FolderContentResponse response = folderService.getFolderContents(user, parentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Folder contents retrieved successfully"));
    }

    @PutMapping("/{id}/rename")
    @Operation(summary = "Rename folder", description = "Renames an existing folder")
    public ResponseEntity<ApiResponse<FolderDto>> renameFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody RenameItemRequest request
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FolderDto folder = folderService.renameFolder(user, id, request.getNewName());
        return ResponseEntity.ok(ApiResponse.success(folder, "Folder renamed successfully"));
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "Move folder", description = "Moves folder into target parent folder")
    public ResponseEntity<ApiResponse<FolderDto>> moveFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody MoveItemRequest request
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FolderDto folder = folderService.moveFolder(user, id, request.getTargetFolderId());
        return ResponseEntity.ok(ApiResponse.success(folder, "Folder moved successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete folder", description = "Soft-deletes folder and recursively trashes contents")
    public ResponseEntity<ApiResponse<String>> deleteFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        folderService.deleteFolder(user, id);
        return ResponseEntity.ok(ApiResponse.success("Folder soft-deleted successfully", "Folder deleted"));
    }
}
