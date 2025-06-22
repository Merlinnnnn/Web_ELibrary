package com.spkt.libraSys.service.notification;

import com.spkt.libraSys.service.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationFactory {
    private final NotificationMapper notificationMapper;

    public NotificationEntity createNotification(UserEntity user, String title, String content, String entityId, String entityType ){
        return NotificationEntity.builder()
                .user(user)
                .title(title)
                .content(content)
                .entityId(entityId)
                .entityType(entityType)
                .status(NotificationEntity.NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public NotificationEntity createNotification(UserEntity user, NotificationType type,
                                                 Map<String, Object> parameters, String entityId, String entityType) {
        return NotificationEntity.builder()
                .user(user)
                .title(type.getTitle())
                .content(type.formatContent(parameters))
                .entityId(entityId)
                .entityType(entityType)
                .status(NotificationEntity.NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();
    }
}