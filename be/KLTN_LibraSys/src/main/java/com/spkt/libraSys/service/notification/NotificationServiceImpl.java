package com.spkt.libraSys.service.notification;

import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.webSocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService, NotificationManagementService {
    private final NotificationRepository notificationRepository;
    private final WebSocketService webSocketService;
    private final NotificationMapper notificationMapper;
    private final NotificationFactory notificationFactory;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    public NotificationResponse createAndSendNotification(String userId, String title, String content) {
        try {
            UserEntity user = getUserOrThrow(userId);
            NotificationEntity notification = notificationFactory.createNotification(user, title, content,null,null);
            return saveAndSendNotification(notification);
        } catch (Exception e) {
            handleNotificationError("Tạo và gửi thông báo", userId, e);
            throw new NotificationException(ErrorCode.NOTIFICATION_SENDING_ERROR, "Không thể gửi thông báo");
        }
    }

    @Override
    public NotificationResponse createAndSendNotification(String userId, NotificationType type,
                                                          Map<String, Object> parameters,String entityId,String entityType) {
        try {
            UserEntity user = getUserOrThrow(userId);
            NotificationEntity notification = notificationFactory.createNotification(user, type, parameters,entityId,entityType);
            return saveAndSendNotification(notification);
        } catch (Exception e) {
            handleNotificationError("Tạo và gửi thông báo theo loại", userId, e);
            throw new NotificationException(ErrorCode.NOTIFICATION_SENDING_ERROR, "Không thể gửi thông báo");
        }
    }

    @Override
    public NotificationResponse saveAndSendNotification(String userId, NotificationResponse notificationResponse) {
        try {
            UserEntity user = getUserOrThrow(userId);
            NotificationEntity notification = notificationMapper.toEntity(notificationResponse, user);
            return saveAndSendNotification(notification);
        } catch (Exception e) {
            handleNotificationError("Lưu và gửi thông báo", userId, e);
            throw new NotificationException(ErrorCode.NOTIFICATION_SENDING_ERROR, "Không thể gửi thông báo");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForCurrentUser(Pageable pageable) {
        UserEntity currentUser = authService.getCurrentUser();
        return notificationRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        NotificationEntity notification = getAndValidateNotification(notificationId);
        notification.setStatus(NotificationEntity.NotificationStatus.READ);
        NotificationEntity savedNotification = notificationRepository.save(notification);
        return notificationMapper.toResponse(savedNotification);
    }

    @Override
    public void markAllRead() {
        UserEntity currentUser = authService.getCurrentUser();
        notificationRepository.markAllAsReadForUser(currentUser.getUserId());
    }

    @Override
    public void deleteNotification(Long notificationId) {
        NotificationEntity notification = getAndValidateNotification(notificationId);
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId) {
        NotificationEntity notification = getAndValidateNotification(notificationId);
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadNotificationCountForCurrentUser() {
        UserEntity currentUser = authService.getCurrentUser();
        return notificationRepository.countByUserAndStatus(
                currentUser,
                NotificationEntity.NotificationStatus.UNREAD
        );
    }

    @Override
    public void createNotifications(NotificationCreateRequest request) {
        try {
            request.getUserIds().forEach(userId -> {
                try {
                    createAndSendNotification(userId, request.getTitle(), request.getContent());
                } catch (Exception e) {
                    log.error("Lỗi khi gửi thông báo cho user {}: {}", userId, e.getMessage());
                }
            });
        } catch (Exception e) {
            handleNotificationError("Tạo nhiều thông báo", null, e);
            throw new NotificationException(ErrorCode.NOTIFICATION_SENDING_ERROR,
                    "Không thể tạo thông báo hàng loạt");
        }
    }

    // Private helper methods
    private UserEntity getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(ErrorCode.USER_NOT_FOUND,
                        "Không tìm thấy người dùng"));
    }

    private NotificationEntity getAndValidateNotification(Long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND,
                        "Không tìm thấy thông báo"));

        UserEntity currentUser = authService.getCurrentUser();
        if (!notification.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new NotificationException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "Không có quyền truy cập thông báo này");
        }

        return notification;
    }

    private NotificationResponse saveAndSendNotification(NotificationEntity notification) {
        NotificationEntity savedNotification = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(savedNotification);
        webSocketService.sendNotificationToUser(notification.getUser().getUserId(), response);
        return response;
    }

    private void handleNotificationError(String operation, String userId, Exception e) {
        String errorMessage = String.format("Lỗi khi %s", operation);
        if (userId != null) {
            errorMessage += String.format(" cho user %s", userId);
        }
        errorMessage += String.format(": %s", e.getMessage());
        log.error(errorMessage);

        if (e instanceof NotificationException) {
            throw (NotificationException) e;
        }
    }
}