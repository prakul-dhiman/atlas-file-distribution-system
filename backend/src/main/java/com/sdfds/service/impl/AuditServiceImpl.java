package com.sdfds.service.impl;

import com.sdfds.dto.AuditLogDto;
import com.sdfds.entity.AuditLog;
import com.sdfds.entity.User;
import com.sdfds.repository.AuditLogRepository;
import com.sdfds.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    private static final DateTimeFormatter CSV_DATE_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final AuditLogRepository auditLogRepository;

    @Async
    @Override
    @Transactional
    public void log(User user, String eventType, String description, String ipAddress,
                    String userAgent, String resourceType, Long resourceId, String severity) {
        try {
            AuditLog entry = AuditLog.builder()
                    .user(user)
                    .eventType(eventType)
                    .eventDescription(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .severity(severity != null ? severity : "INFO")
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to persist audit log event={} user={}: {}", eventType,
                    user != null ? user.getUsername() : "anonymous", e.getMessage());
        }
    }

    @Async
    @Override
    @Transactional
    public void logAnonymous(String eventType, String description, String ipAddress,
                              String userAgent, String severity) {
        log(null, eventType, description, ipAddress, userAgent, null, null, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getLogsByUser(User user, Pageable pageable) {
        return auditLogRepository.findByUserOrderByCreatedAtDesc(user, pageable).map(this::toDto);
    }

    private AuditLogDto toDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : null)
                .eventType(log.getEventType())
                .eventDescription(log.getEventDescription())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .geoCountry(log.getGeoCountry())
                .geoCity(log.getGeoCity())
                .severity(log.getSeverity())
                .createdAt(log.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> exportLogs(Instant from, Instant to) {
        return auditLogRepository.findByDateRange(
                from != null ? from : Instant.EPOCH,
                to != null ? to : Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public String exportToCsv(Instant from, Instant to) {
        List<AuditLog> logs = exportLogs(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append("id,timestamp,user,event_type,description,ip_address,resource_type,resource_id,severity\n");
        for (AuditLog entry : logs) {
            sb.append(csvEscape(String.valueOf(entry.getId()))).append(",");
            sb.append(csvEscape(CSV_DATE_FMT.format(entry.getCreatedAt()))).append(",");
            sb.append(csvEscape(entry.getUser() != null ? entry.getUser().getUsername() : "anonymous")).append(",");
            sb.append(csvEscape(entry.getEventType())).append(",");
            sb.append(csvEscape(entry.getEventDescription())).append(",");
            sb.append(csvEscape(entry.getIpAddress())).append(",");
            sb.append(csvEscape(entry.getResourceType())).append(",");
            sb.append(csvEscape(entry.getResourceId() != null ? String.valueOf(entry.getResourceId()) : "")).append(",");
            sb.append(csvEscape(entry.getSeverity())).append("\n");
        }
        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
