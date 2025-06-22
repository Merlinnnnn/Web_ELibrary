package com.spkt.libraSys.service.notification;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationCreateRequest {

    @NotNull(message = "Danh sách ID người dùng không được để trống")
    List<String> userIds;

    @NotNull(message = "Tiêu đề không được để trống")
    String title;

    @NotNull(message = "Nội dung không được để trống")
    String content;
}