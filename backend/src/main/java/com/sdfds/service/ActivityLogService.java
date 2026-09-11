package com.sdfds.service;

import com.sdfds.entity.User;

public interface ActivityLogService {

    void log(User user, String action, String resourceType, Long resourceId, String details, String ipAddress);

    void log(User user, String action, String resourceType, Long resourceId, String details);
}