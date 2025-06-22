package com.spkt.libraSys.service.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

/**
 * Service interface for managing notifications in the library system.
 * Provides functionality for creating, sending, and managing notifications.
 */
public interface NotificationService extends BaseNotificationService {
    /**
     * Creates and sends a simple notification with title and content.
     * @param userId Target user ID
     * @param title Notification title
     * @param content Notification content
     * @return Created notification response
     */
    NotificationResponse createAndSendNotification(String userId, String title, String content);
    
    /**
     * Creates and sends a typed notification with parameters.
     * @param userId Target user ID
     * @param type Notification type
     * @param parameters Additional parameters for notification
     * @param entityId Related entity ID
     * @param entityType Type of related entity
     * @return Created notification response
     */
    NotificationResponse createAndSendNotification(String userId, NotificationType type, 
            Map<String, Object> parameters, String entityId, String entityType);
    
    /**
     * Saves and sends a notification from a response object.
     * @param userId Target user ID
     * @param notification Notification data to save and send
     * @return Saved notification response
     */
    NotificationResponse saveAndSendNotification(String userId, NotificationResponse notification);
    
    /**
     * Retrieves notifications for the current user with pagination.
     * @param pageable Pagination parameters
     * @return Page of notifications
     */
    Page<NotificationResponse> getNotificationsForCurrentUser(Pageable pageable);
    
    /**
     * Gets the count of unread notifications for the current user.
     * @return Count of unread notifications
     */
    Long getUnreadNotificationCountForCurrentUser();
}