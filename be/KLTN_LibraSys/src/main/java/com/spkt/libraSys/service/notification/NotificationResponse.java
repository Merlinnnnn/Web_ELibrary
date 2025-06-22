package com.spkt.libraSys.service.notification;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    String id;         
    String username;     
    String title;      
    String content;    
    String entityId;
    String entityType;
    LocalDateTime createdAt; 
    NotificationEntity.NotificationStatus status; 
}