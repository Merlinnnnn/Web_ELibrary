package com.spkt.libraSys.service.notification;


import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.notification.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification Controller", description = "API để quản lý thông báo")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationManagementService notificationManagementService;

    @GetMapping
    @Operation(summary = "Lấy thông báo của người dùng hiện tại",
            description = "Trả về danh sách thông báo có phân trang của người dùng đang đăng nhập")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getCurrentUserNotifications(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        Page<NotificationResponse> notifications = notificationService.getNotificationsForCurrentUser(pageable);
        return ResponseEntity.ok(ApiResponse.<Page<NotificationResponse>>builder()
                .message("Lấy danh sách thông báo thành công")
                .data(notifications)
                .build());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả thông báo (Admin)")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAllNotifications(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> notifications = notificationManagementService.getAllNotifications(pageable);
        return ResponseEntity.ok(ApiResponse.<Page<NotificationResponse>>builder()
                .message("Lấy tất cả thông báo thành công")
                .data(notifications)
                .build());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Đếm số thông báo chưa đọc")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        Long count = notificationService.getUnreadNotificationCountForCurrentUser();
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .message("Lấy số thông báo chưa đọc thành công")
                .data(count)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông báo theo ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @Parameter(description = "ID của thông báo") @PathVariable("id") Long id) {
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .message("Lấy thông báo thành công")
                .data(notification)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo thông báo mới (Admin)")
    public ResponseEntity<ApiResponse<Void>> createNotifications(
            @Valid @RequestBody NotificationCreateRequest request) {
        notificationManagementService.createNotifications(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .message("Tạo thông báo thành công")
                        .build());
    }

    @PostMapping("/send/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gửi thông báo cho người dùng cụ thể (Admin)")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotificationToUser(
            @PathVariable String userId,
            @RequestParam NotificationType type,
            @RequestBody Map<String, Object> parameters) {
        NotificationResponse notification = notificationService.createAndSendNotification(
                userId, type, parameters,null,null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<NotificationResponse>builder()
                        .message("Gửi thông báo thành công")
                        .data(notification)
                        .build());
    }

    @PatchMapping("/{id}/mark-read")
    @Operation(summary = "Đánh dấu thông báo đã đọc")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @Parameter(description = "ID của thông báo") @PathVariable("id") Long id) {
        NotificationResponse notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .message("Đánh dấu thông báo đã đọc thành công")
                .data(notification)
                .build());
    }

    @PatchMapping("/mark-all-read")
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đánh dấu tất cả thông báo đã đọc thành công")
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thông báo")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @Parameter(description = "ID của thông báo") @PathVariable("id") Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Xóa thông báo thành công")
                .build());
    }
}