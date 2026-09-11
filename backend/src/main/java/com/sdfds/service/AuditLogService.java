package com.sdfds.service;

import com.sdfds.entity.User;

public interface AuditLogService {

    void log(String eventType, String eventDescription, User user, String ipAddress, String userAgent);

    void log(String eventType, String eventDescription, User user);
}