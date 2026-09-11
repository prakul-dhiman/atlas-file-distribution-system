package com.sdfds.controller;

import com.sdfds.dto.ApiResponse;
import com.sdfds.dto.AuditLogDto;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.AuditService;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Audit Logs & Compliance")
public class AuditController {

    private final AuditService auditService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get paginated audit logs (admin view)")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogs(pageable), "Audit logs retrieved"));
    }

    @GetMapping("/mine")
    @Operation(summary = "Get audit logs for the current user")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getMyLogs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.getUserEntity(userPrincipal.getId());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogsByUser(user, pageable), "Your audit logs retrieved"));
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit logs as CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant fromInstant = from != null ? Instant.parse(from) : Instant.now().minusSeconds(86400 * 30L);
        Instant toInstant = to != null ? Instant.parse(to) : Instant.now();
        String csv = auditService.exportToCsv(fromInstant, toInstant);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
                .body(csv.getBytes());
    }
}
