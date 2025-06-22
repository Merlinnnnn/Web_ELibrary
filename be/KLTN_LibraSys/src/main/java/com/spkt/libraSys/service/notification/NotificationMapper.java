package com.spkt.libraSys.service.notification;

import com.spkt.libraSys.service.user.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId().toString())
                .title(entity.getTitle())
                .content(entity.getContent())
                .entityId(entity.getEntityId())
                .entityType(entity.getEntityType())
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus())
                .username(entity.getUser().getUsername())
                .build();
    }

    public NotificationEntity toEntity(NotificationResponse response, UserEntity user) {
        return NotificationEntity.builder()
                .user(user)
                .title(response.getTitle())
                .content(response.getContent())
                .status(NotificationEntity.NotificationStatus.UNREAD)
                .build();
    }
}