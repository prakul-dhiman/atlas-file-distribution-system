package com.sdfds.service;

import com.sdfds.dto.AuditLogDto;
import com.sdfds.entity.AuditLog;
import com.sdfds.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface AuditService {

    void log(User user, String eventType, String description, String ipAddress, String userAgent,
             String resourceType, Long resourceId, String severity);

    void logAnonymous(String eventType, String description, String ipAddress, String userAgent, String severity);

    Page<AuditLogDto> getLogs(Pageable pageable);

    Page<AuditLogDto> getLogsByUser(User user, Pageable pageable);

    List<AuditLog> exportLogs(Instant from, Instant to);

    String exportToCsv(Instant from, Instant to);
}
