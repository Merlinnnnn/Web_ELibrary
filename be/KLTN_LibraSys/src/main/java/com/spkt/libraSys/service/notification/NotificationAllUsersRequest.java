package com.spkt.libraSys.service.notification;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationAllUsersRequest {

    @NotNull(message = "Tiêu đề không được để trống")
    String title;

    @NotNull(message = "Nội dung không được để trống")
    String content;

    String groupName; // Optional: To send to specific group (e.g., "ADMIN", "USER")
}