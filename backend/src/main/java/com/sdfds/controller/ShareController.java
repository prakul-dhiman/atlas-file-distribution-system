package com.sdfds.controller;

import com.sdfds.dto.*;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.FileService;
import com.sdfds.service.ShareOtpService;
import com.sdfds.service.ShareService;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
@Tag(name = "Sharing")
public class ShareController {

    private final ShareService shareService;
    private final UserService userService;
    private final ShareOtpService shareOtpService;

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create share link")
    public ResponseEntity<ApiResponse<SharedLinkDto>> createShare(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateSharedLinkRequest request) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(shareService.createShareLink(user, request), "Share link created successfully"));
    }

    @GetMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "List my shares")
    public ResponseEntity<ApiResponse<List<SharedLinkDto>>> listShares(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(shareService.listUserShares(user), "Shares retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Revoke share link")
    public ResponseEntity<ApiResponse<String>> revokeShare(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        User user = userService.getUserEntity(userPrincipal.getId());
        shareService.revokeShare(user, id);
        return ResponseEntity.ok(ApiResponse.success("Share link revoked", "Share revoked successfully"));
    }

    @GetMapping("/public/{token}")
    @Operation(summary = "Get shared link metadata")
    public ResponseEntity<ApiResponse<SharedLinkDto>> getShareMetadata(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(shareService.getSharedLinkMetadata(token), "Metadata retrieved"));
    }

    @PostMapping("/public/{token}/access")
    @Operation(summary = "Access shared content")
    public ResponseEntity<ApiResponse<Object>> accessSharedContent(
            @PathVariable String token,
            @RequestBody(required = false) VerifySharePasswordRequest request) {
        String password = request != null ? request.getPassword() : null;
        return ResponseEntity.ok(ApiResponse.success(shareService.getSharedLinkContents(token, password), "Content retrieved"));
    }

    @PostMapping("/public/{token}/sign-download")
    @Operation(summary = "Generate signed download URL")
    public ResponseEntity<ApiResponse<String>> generateSignedUrl(
            @PathVariable String token,
            @RequestParam Long fileId,
            @RequestBody(required = false) VerifySharePasswordRequest request) {
        String password = request != null ? request.getPassword() : null;
        return ResponseEntity.ok(ApiResponse.success(
                shareService.generateSignedDownloadUrl(token, fileId, password), "Signed URL generated"));
    }

    @PostMapping("/public/{token}/otp/request")
    @Operation(summary = "Request OTP for email-protected share")
    public ResponseEntity<ApiResponse<String>> requestOtp(
            @PathVariable String token,
            @RequestBody RequestOtpRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null) ip = httpRequest.getRemoteAddr();
        shareOtpService.sendOtp(token, request.getEmail(), ip);
        return ResponseEntity.ok(ApiResponse.success("OTP sent to " + request.getEmail(), "OTP sent successfully"));
    }

    @PostMapping("/public/{token}/otp/verify")
    @Operation(summary = "Verify OTP for email-protected share")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyOtp(
            @PathVariable String token,
            @RequestBody VerifyOtpRequest request) {
        String sessionToken = shareOtpService.verifyOtp(token, request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(Map.of("sessionToken", sessionToken), "OTP verified successfully"));
    }

    @GetMapping("/download/{token}")
    @Operation(summary = "Download via signed URL")
    public ResponseEntity<Resource> downloadSharedFile(
            @PathVariable String token,
            @RequestParam Long fileId,
            @RequestParam long expires,
            @RequestParam String signature,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null) ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");

        FileService.FileDownloadResource dl =
                shareService.downloadSharedFile(token, fileId, expires, signature, ip, ua);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dl.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dl.filename() + "\"")
                .contentLength(dl.sizeBytes())
                .body(dl.resource());
    }
}
