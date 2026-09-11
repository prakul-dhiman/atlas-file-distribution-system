package com.sdfds.controller;

import com.sdfds.dto.ApiResponse;
import com.sdfds.dto.AnalyticsOverviewDto;
import com.sdfds.entity.User;
import com.sdfds.security.UserPrincipal;
import com.sdfds.service.AnalyticsService;
import com.sdfds.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService userService;

    @GetMapping("/overview")
    @Operation(summary = "Get user download analytics overview dashboard data")
    public ResponseEntity<ApiResponse<AnalyticsOverviewDto>> getOverview(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userService.getUserEntity(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOverview(user), "Analytics overview fetched"));
    }

    @GetMapping("/export")
    @Operation(summary = "Export analytics report as CSV")
    public ResponseEntity<byte[]> exportReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userService.getUserEntity(userPrincipal.getId());
        byte[] csvData = analyticsService.exportReportCsv(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download-analytics-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
