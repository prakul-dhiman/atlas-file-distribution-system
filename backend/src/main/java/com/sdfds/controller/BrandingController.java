package com.sdfds.controller;

import com.sdfds.dto.ApiResponse;
import com.sdfds.dto.BrandingDto;
import com.sdfds.entity.User;
import com.sdfds.repository.SharedLinkRepository;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.UserService;
import com.sdfds.service.impl.BrandingServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/branding")
@RequiredArgsConstructor
@Tag(name = "Branded Share Portals")
public class BrandingController {

    private final BrandingServiceImpl brandingService;
    private final UserService userService;
    private final SharedLinkRepository sharedLinkRepository;

    @GetMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get my branding settings")
    public ResponseEntity<ApiResponse<BrandingDto>> getMyBranding(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(brandingService.getBranding(user), "Branding retrieved"));
    }

    @PutMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create or update my branding settings")
    public ResponseEntity<ApiResponse<BrandingDto>> upsertBranding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody BrandingDto dto) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(brandingService.upsertBranding(user, dto), "Branding updated"));
    }

    @GetMapping("/public/{shareToken}")
    @Operation(summary = "Get branding for a public share (used by share page)")
    public ResponseEntity<ApiResponse<BrandingDto>> getShareBranding(
            @PathVariable String shareToken) {
        return sharedLinkRepository.findByToken(shareToken)
                .map(link -> {
                    BrandingDto branding = brandingService.getBrandingForShare(link.getUser());
                    return ResponseEntity.ok(ApiResponse.success(branding, "Branding retrieved"));
                })
                .orElse(ResponseEntity.ok(ApiResponse.success(
                        BrandingDto.builder().primaryColor("#6366f1").accentColor("#8b5cf6")
                                .backgroundColor("#0f172a").showPoweredBy(true).build(),
                        "Default branding")));
    }
}
