package com.spkt.libraSys.service.notification;

public interface BaseNotificationService {
    NotificationResponse markAsRead(Long notificationId);
    void markAllRead();
    void deleteNotification(Long notificationId);
    NotificationResponse getNotificationById(Long notificationId);
}