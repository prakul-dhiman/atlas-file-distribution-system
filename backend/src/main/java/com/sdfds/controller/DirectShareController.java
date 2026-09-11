package com.sdfds.controller;

import com.sdfds.dto.*;
import com.sdfds.entity.DirectItemShare.PermissionLevel;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.DirectShareService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/direct-shares")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Direct Sharing & RBAC")
public class DirectShareController {

    private final DirectShareService directShareService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Share a file or folder directly with another user")
    public ResponseEntity<ApiResponse<DirectShareDto>> createDirectShare(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateDirectShareRequest request) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(directShareService.createDirectShare(user, request), "Share created successfully"));
    }

    @GetMapping("/shared-with-me")
    @Operation(summary = "Get all items shared with the current user")
    public ResponseEntity<ApiResponse<List<DirectShareDto>>> getSharedWithMe(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(directShareService.getSharedWithMe(user), "Shared items retrieved"));
    }

    @GetMapping("/collaborators")
    @Operation(summary = "Get all collaborators on a specific file or folder")
    public ResponseEntity<ApiResponse<List<DirectShareDto>>> getCollaborators(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Long fileId,
            @RequestParam(required = false) Long folderId) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(
                directShareService.getItemCollaborators(user, fileId, folderId), "Collaborators retrieved"));
    }

    @PatchMapping("/{shareId}/permission")
    @Operation(summary = "Update the permission level for a share")
    public ResponseEntity<ApiResponse<DirectShareDto>> updatePermission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long shareId,
            @RequestBody Map<String, String> body) {
        User user = userService.getUserEntity(userPrincipal.getId());
        PermissionLevel level = PermissionLevel.valueOf(body.get("permissionLevel").toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(directShareService.updatePermission(user, shareId, level), "Permission updated"));
    }

    @DeleteMapping("/{shareId}")
    @Operation(summary = "Revoke a direct share")
    public ResponseEntity<ApiResponse<String>> revokeShare(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long shareId) {
        User user = userService.getUserEntity(userPrincipal.getId());
        directShareService.revokeShare(user, shareId);
        return ResponseEntity.ok(ApiResponse.success("Share revoked", "Share revoked successfully"));
    }
}
