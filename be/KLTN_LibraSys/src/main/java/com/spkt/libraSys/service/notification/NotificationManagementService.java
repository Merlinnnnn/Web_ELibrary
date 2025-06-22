package com.spkt.libraSys.service.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NotificationManagementService extends BaseNotificationService {
    @PreAuthorize("hasRole('ADMIN')")
    Page<NotificationResponse> getAllNotifications(Pageable pageable);
    
    @PreAuthorize("hasRole('ADMIN')")
    void createNotifications(NotificationCreateRequest request);
}