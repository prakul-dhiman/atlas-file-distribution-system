package com.sdfds.controller;

import com.sdfds.dto.ApiResponse;
import com.sdfds.dto.UserDto;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and account operations")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Returns details of currently authenticated user")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserDto userDto = userService.getCurrentUserProfile(userPrincipal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(userDto, "User profile retrieved successfully"));
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile image", description = "Uploads and saves a profile picture for the current user")
    public ResponseEntity<ApiResponse<UserDto>> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        UserDto updated = userService.uploadProfileImage(user, file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(updated, "Profile image uploaded successfully"));
    }

    @GetMapping("/{userId}/profile-image")
    @Operation(summary = "Get profile image", description = "Serves a user's profile picture (authenticated)")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Long userId) {
        Resource resource = userService.loadProfileImage(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password", description = "Allows an authenticated user to change their password")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @jakarta.validation.Valid @RequestBody com.sdfds.dto.ChangePasswordRequest request
    ) {
        User user = userService.getUserEntity(userPrincipal.getId());
        userService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", "Password changed"));
    }
}