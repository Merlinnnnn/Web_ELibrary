package com.spkt.libraSys.service.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    String oldPassword;


    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    String newPassword;

    @NotBlank(message = "Confirm new password is required")
    @Size(min = 8, message = "Confirm new password must be at least 8 characters long")
    String confirmNewPassword;
}