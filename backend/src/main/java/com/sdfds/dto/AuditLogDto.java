package com.sdfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String username;
    private String eventType;
    private String eventDescription;
    private String ipAddress;
    private String userAgent;
    private String resourceType;
    private Long resourceId;
    private String geoCountry;
    private String geoCity;
    private String severity;
    private Instant createdAt;
}
