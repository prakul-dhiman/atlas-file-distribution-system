package com.sdfds.controller;

import com.sdfds.dto.*;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.RecycleBinService;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recycle-bin")
@RequiredArgsConstructor
@Tag(name = "Recycle Bin", description = "View, restore, and permanently purge trashed files and folders")
@SecurityRequirement(name = "Bearer Authentication")
public class RecycleBinController {

    private final RecycleBinService recycleBinService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "List trashed items", description = "Lists all soft-deleted files and folders for the authenticated user")
    public ResponseEntity<ApiResponse<RecycleBinResponse>> listTrashedItems(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        RecycleBinResponse response = recycleBinService.listTrashedItems(user);
        return ResponseEntity.ok(ApiResponse.success(response, "Trashed items retrieved successfully"));
    }

    @PostMapping("/folders/{id}/restore")
    @Operation(summary = "Restore folder", description = "Restores a trashed folder. If its parent is still trashed, restores to root.")
    public ResponseEntity<ApiResponse<FolderDto>> restoreFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FolderDto restored = recycleBinService.restoreFolder(user, id);
        return ResponseEntity.ok(ApiResponse.success(restored, "Folder restored successfully"));
    }

    @PostMapping("/files/{id}/restore")
    @Operation(summary = "Restore file", description = "Restores a trashed file. If its parent folder is still trashed, restores to root.")
    public ResponseEntity<ApiResponse<FileDto>> restoreFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto restored = recycleBinService.restoreFile(user, id);
        return ResponseEntity.ok(ApiResponse.success(restored, "File restored successfully"));
    }

    @DeleteMapping("/folders/{id}/purge")
    @Operation(summary = "Purge folder permanently", description = "Permanently deletes a trashed folder and all its contents from storage")
    public ResponseEntity<ApiResponse<String>> purgeFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        recycleBinService.purgeFolder(user, id);
        return ResponseEntity.ok(ApiResponse.success("Folder permanently purged", "Folder purged successfully"));
    }

    @DeleteMapping("/files/{id}/purge")
    @Operation(summary = "Purge file permanently", description = "Permanently deletes a trashed file from database and physical storage")
    public ResponseEntity<ApiResponse<String>> purgeFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        recycleBinService.purgeFile(user, id);
        return ResponseEntity.ok(ApiResponse.success("File permanently purged", "File purged successfully"));
    }

    @DeleteMapping("/empty")
    @Operation(summary = "Empty trash", description = "Permanently deletes all trashed items for the authenticated user")
    public ResponseEntity<ApiResponse<String>> emptyTrash(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        recycleBinService.emptyTrash(user);
        return ResponseEntity.ok(ApiResponse.success("Trash emptied", "All trashed items permanently deleted"));
    }
}
