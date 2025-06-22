package com.spkt.libraSys.service.notification;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationUpdateRequest {
    String status; // Trạng thái mới của thông báo, ví dụ: "READ", "UNREAD"
}