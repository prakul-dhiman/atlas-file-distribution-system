package com.sdfds.service.impl;

import com.sdfds.entity.Notification;
import com.sdfds.entity.User;
import com.sdfds.repository.NotificationRepository;
import com.sdfds.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(User user, String title, String message, String type) {
        try {
            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(type == null ? "INFO" : type)
                    .build();
            notificationRepository.save(notification);
            log.debug("Notification sent to user {}: {}", user.getId(), title);
        } catch (Exception e) {
            log.error("Failed to create notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                });
    }
}