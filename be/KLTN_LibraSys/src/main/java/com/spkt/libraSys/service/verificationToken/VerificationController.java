package com.spkt.libraSys.service.verificationToken;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.access.VerificationResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/auth")
public class VerificationController {

    VerificationService verificationService;

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<VerificationResponse>> verifyAccount(@RequestParam String token) {
        VerificationResponse verificationResponse = verificationService.verifyAccount(token);

        boolean isVerified = verificationResponse.isVerified();

        ApiResponse<VerificationResponse> response = ApiResponse.<VerificationResponse>builder()
                .success(isVerified)
                .message(isVerified ? "Tài khoản đã được xác minh thành công" : "Mã xác minh không hợp lệ hoặc đã hết hạn")
                .data(verificationResponse)
                .build();

        return ResponseEntity.ok(response);
    }
    @GetMapping("/resend-verification-code")
    public ResponseEntity<ApiResponse<Boolean>> resendVerificationCode(@RequestParam String email) {
        boolean isResent = verificationService.resendVerificationCode(email);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(isResent)
                .message(isResent ? "Mã xác minh đã được gửi lại" : "Không tìm thấy người dùng với email: " + email)
                .data(isResent)
                .build();
        return ResponseEntity.ok(response);
    }
    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@RequestBody Map<String,String> request   ) {
        String email = request.get("email");
        if(email == null || email.isBlank()){
            return ResponseEntity.badRequest().body(
                ApiResponse.<Void>builder()
                    .success(false)
                    .message("Email không được để trống")
                    .build()
            );
        }
        verificationService.requestPasswordReset(email);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Yêu cầu đặt lại mật khẩu đã được gửi lại")
                .build();
        return ResponseEntity.ok(response);
    }
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request){
        verificationService.resetPassword(request);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Đặt lại mật khẩu thành công")
                .build();
        return ResponseEntity.ok(response);
    }
}
