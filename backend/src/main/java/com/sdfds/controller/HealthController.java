package com.sdfds.controller;

import com.sdfds.dto.ApiResponse;
import com.sdfds.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "System Health & Operational Status API")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping
    @Operation(summary = "Get system health status", description = "Returns overall health status of Project Atlas backend services")
    public ResponseEntity<ApiResponse<HealthResponse>> getHealthStatus() {
        log.info("System health check requested");
        HealthResponse health = HealthResponse.builder()
                .status("UP")
                .version("1.0.0-SNAPSHOT")
                .timestamp(Instant.now())
                .components(Map.of(
                        "database", "UP",
                        "redis", "UP",
                        "storageEngine", "READY"
                ))
                .build();
        return ResponseEntity.ok(ApiResponse.success(health, "System is healthy"));
    }
}
