package com.sdfds.service.impl;

import com.sdfds.entity.AuditLog;
import com.sdfds.entity.User;
import com.sdfds.repository.AuditLogRepository;
import com.sdfds.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String eventDescription, User user, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .eventType(eventType)
                    .eventDescription(eventDescription)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            auditLogRepository.save(auditLog);
            log.info("Audit event [{}]: {}", eventType, eventDescription);
        } catch (Exception e) {
            // Audit logging must never break the main request flow
            log.error("Failed to write audit log for event [{}]: {}", eventType, e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String eventDescription, User user) {
        log(eventType, eventDescription, user, null, null);
    }
}