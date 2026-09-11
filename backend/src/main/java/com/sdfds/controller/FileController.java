package com.sdfds.controller;

import com.sdfds.dto.*;
import com.sdfds.entity.User;
import com.sdfds.mapper.FileMapper;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.FileService;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload, download streaming, metadata lookup, rename, move, copy, and soft deletion")
@SecurityRequirement(name = "Bearer Authentication")
public class FileController {

    private final FileService fileService;
    private final UserService userService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Uploads a single file to specified folder or root directory")
    public ResponseEntity<ApiResponse<FileDto>> uploadFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto uploadedFile = fileService.uploadFile(user, file, folderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(uploadedFile, "File uploaded successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file metadata", description = "Retrieves file details by ID")
    public ResponseEntity<ApiResponse<FileDto>> getFileMetadata(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto fileDto = fileService.getFileMetadata(user, id);
        return ResponseEntity.ok(ApiResponse.success(fileDto, "File metadata retrieved successfully"));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "List file versions", description = "Returns all versions for the selected file name in its folder")
    public ResponseEntity<ApiResponse<List<FileDto>>> getFileVersions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        List<FileDto> versions = fileService.getFileVersionHistory(user, id);
        return ResponseEntity.ok(ApiResponse.success(versions, "File versions retrieved successfully"));
    }

    @PostMapping("/{id}/versions/{version}/restore")
    @Operation(summary = "Restore file version", description = "Creates a new latest version from an older version")
    public ResponseEntity<ApiResponse<FileDto>> restoreFileVersion(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @PathVariable Integer version
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto restored = fileService.restoreFileVersion(user, id, version);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(restored, "File version restored successfully"));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download file stream", description = "Streams raw file content with Content-Disposition headers")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileService.FileDownloadResource download = fileService.downloadFile(user, id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentLength(download.sizeBytes())
                .body(download.resource());
    }

    @PutMapping("/{id}/rename")
    @Operation(summary = "Rename file", description = "Renames an existing file")
    public ResponseEntity<ApiResponse<FileDto>> renameFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody RenameItemRequest request
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto fileDto = fileService.renameFile(user, id, request.getNewName());
        return ResponseEntity.ok(ApiResponse.success(fileDto, "File renamed successfully"));
    }

    @PutMapping("/{id}/move")
    @Operation(summary = "Move file", description = "Moves file into target folder")
    public ResponseEntity<ApiResponse<FileDto>> moveFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody MoveItemRequest request
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto fileDto = fileService.moveFile(user, id, request.getTargetFolderId());
        return ResponseEntity.ok(ApiResponse.success(fileDto, "File moved successfully"));
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "Copy file", description = "Creates a copy of file in target folder")
    public ResponseEntity<ApiResponse<FileDto>> copyFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody MoveItemRequest request
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto fileDto = fileService.copyFile(user, id, request.getTargetFolderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(fileDto, "File copied successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete file", description = "Soft-deletes file (moves to trash)")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        fileService.deleteFile(user, id);
        return ResponseEntity.ok(ApiResponse.success("File soft-deleted successfully", "File deleted"));
    }

    @PutMapping("/{id}/star")
    @Operation(summary = "Toggle star status", description = "Toggles star/favorite status of a file")
    public ResponseEntity<ApiResponse<FileDto>> toggleStar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        FileDto fileDto = fileService.toggleStar(user, id);
        return ResponseEntity.ok(ApiResponse.success(fileDto, "File star status updated"));
    }

    @GetMapping("/starred")
    @Operation(summary = "List starred files", description = "Retrieves all starred files for the current user")
    public ResponseEntity<ApiResponse<List<FileDto>>> getStarredFiles(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        List<FileDto> starred = fileService.getStarredFiles(user);
        return ResponseEntity.ok(ApiResponse.success(starred, "Starred files retrieved"));
    }

    @PostMapping("/download-zip")
    @Operation(summary = "Download files as Zip", description = "Bundles multiple selected files into a single zip download")
    public ResponseEntity<byte[]> downloadZip(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<Long> fileIds
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        byte[] zipBytes = fileService.downloadFilesAsZip(user, fileIds);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"atlas_files.zip\"")
                .contentLength(zipBytes.length)
                .body(zipBytes);
    }
}
