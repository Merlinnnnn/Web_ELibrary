package com.spkt.libraSys.service.notification;

import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.webSocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationFactory notificationFactory;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserEntity testUser;
    private NotificationEntity testNotification;
    private NotificationResponse testNotificationResponse;
    private NotificationCreateRequest testCreateRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = UserEntity.builder()
                .userId("test123")
                .username("testuser@example.com")
                .build();

        // Setup test notification
        testNotification = NotificationEntity.builder()
                .id(1L)
                .user(testUser)
                .title("Test Notification")
                .content("This is a test notification")
                .status(NotificationEntity.NotificationStatus.UNREAD)
                .build();

        // Setup test notification response
        testNotificationResponse = NotificationResponse.builder()
                .id("1")
                .title("Test Notification")
                .content("This is a test notification")
                .status(NotificationEntity.NotificationStatus.UNREAD)
                .username(testUser.getUsername())
                .build();

        // Setup test create request
        testCreateRequest = NotificationCreateRequest.builder()
                .title("Test Notification")
                .content("This is a test notification")
                .userIds(List.of("test123"))
                .build();
    }

    @Test
    void createAndSendNotification_Success() {
        // Arrange
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));
        when(notificationFactory.createNotification(eq(testUser), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(testNotification);
        when(notificationRepository.save(any())).thenReturn(testNotification);
        when(notificationMapper.toResponse(any())).thenReturn(testNotificationResponse);

        // Act
        NotificationResponse response = notificationService.createAndSendNotification(
                "test123", "Test Notification", "This is a test notification");

        // Assert
        assertNotNull(response);
        assertEquals(testNotificationResponse.getId(), response.getId());
        verify(webSocketService).sendNotificationToUser(eq("test123"), any());
    }

    @Test
    void createAndSendNotification_WithType_Success() {
        // Arrange
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bookName", "Test Book");
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));
        when(notificationFactory.createNotification(eq(testUser), eq(NotificationType.LOAN_CREATED), any(), anyString(), anyString()))
                .thenReturn(testNotification);
        when(notificationRepository.save(any())).thenReturn(testNotification);
        when(notificationMapper.toResponse(any())).thenReturn(testNotificationResponse);

        // Act
        NotificationResponse response = notificationService.createAndSendNotification(
                "test123", NotificationType.LOAN_CREATED, parameters, "entity1", "LOAN");

        // Assert
        assertNotNull(response);
        assertEquals(testNotificationResponse.getId(), response.getId());
        verify(webSocketService).sendNotificationToUser(eq("test123"), any());
    }

    @Test
    void getNotificationsForCurrentUser_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<NotificationEntity> notifications = Collections.singletonList(testNotification);
        Page<NotificationEntity> notificationPage = new PageImpl<>(notifications, pageable, notifications.size());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findByUserOrderByCreatedAtDesc(eq(testUser), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(notificationMapper.toResponse(any())).thenReturn(testNotificationResponse);

        // Act
        Page<NotificationResponse> response = notificationService.getNotificationsForCurrentUser(pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(testNotificationResponse.getId(), response.getContent().get(0).getId());
    }

    @Test
    void getUnreadNotificationCountForCurrentUser_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.countByUserAndStatus(eq(testUser), eq(NotificationEntity.NotificationStatus.UNREAD)))
                .thenReturn(5L);

        // Act
        Long count = notificationService.getUnreadNotificationCountForCurrentUser();

        // Assert
        assertEquals(5L, count);
    }

    @Test
    void markAsRead_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any())).thenReturn(testNotification);
        when(notificationMapper.toResponse(any())).thenReturn(testNotificationResponse);

        // Act
        NotificationResponse response = notificationService.markAsRead(1L);

        // Assert
        assertNotNull(response);
        assertEquals(NotificationEntity.NotificationStatus.READ, testNotification.getStatus());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void markAllRead_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);

        // Act
        notificationService.markAllRead();

        // Assert
        verify(notificationRepository).markAllAsReadForUser("test123");
    }

    @Test
    void deleteNotification_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        // Act
        notificationService.deleteNotification(1L);

        // Assert
        verify(notificationRepository).delete(testNotification);
    }

    @Test
    void createNotifications_Success() {
        // Arrange
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));
        when(notificationFactory.createNotification(eq(testUser), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(testNotification);
        when(notificationRepository.save(any())).thenReturn(testNotification);
        when(notificationMapper.toResponse(any())).thenReturn(testNotificationResponse);

        // Act
        notificationService.createNotifications(testCreateRequest);

        // Assert
        verify(notificationRepository).save(any());
        verify(webSocketService).sendNotificationToUser(eq("test123"), any());
    }

    @Test
    void createAndSendNotification_UserNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () ->
                notificationService.createAndSendNotification("nonexistent", "Test", "Test"));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void markAsRead_NotificationNotFound() {
        // Arrange
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () ->
                notificationService.markAsRead(999L));
        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void markAsRead_UnauthorizedAccess() {
        // Arrange
        UserEntity otherUser = UserEntity.builder()
                .userId("other123")
                .username("other@example.com")
                .build();
        NotificationEntity otherNotification = NotificationEntity.builder()
                .id(1L)
                .user(otherUser)
                .build();

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(otherNotification));

        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () ->
                notificationService.markAsRead(1L));
        assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());
    }
} 