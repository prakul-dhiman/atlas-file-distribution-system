package com.sdfds.service;

import com.sdfds.entity.Notification;
import com.sdfds.entity.User;

import java.util.List;

public interface NotificationService {

    void notify(User user, String title, String message, String type);

    List<Notification> getForUser(Long userId);

    long countUnread(Long userId);

    void markRead(Long notificationId, Long userId);

    void markAllRead(Long userId);
}