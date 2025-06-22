package com.spkt.libraSys.service.access;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleRepository;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/auth")
public class AuthController {

    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse authResponse = authService.login(authRequest);
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Đăng nhập thành công")
                .data(authResponse)
                .build();

        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token không hợp lệ!");
        }

        String token = authHeader.substring(7); // Loại bỏ "Bearer " để lấy token thực sự
        authService.logout(token);

        return ResponseEntity.ok("Đăng xuất thành công!");
    }
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@RequestBody Map<String,String> request){
        String token  = request.get("token");
        log.info(token);
        var result = authService.refresh(token);
        return ApiResponse.<AuthResponse>builder().data(result).build();
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody AuthRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu xác nhận không khớp");
        }
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đăng ký thành công, vui long xac thuat email")
                .build());
    }

    @PostMapping("/login-google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");
        
        if (email == null || name == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.<AuthResponse>builder()
                    .success(false)
                    .message("Email và tên là bắt buộc")
                    .build()
            );
        }

        AuthResponse authResponse = authService.handleGoogleLogin(email, name);
        
        return ResponseEntity.ok(
            ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Đăng nhập thành công")
                .data(authResponse)
                .build()
        );
    }
    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<Map<String,Boolean>>> introspect(@RequestBody Map<String,String> request) {
       String token = request.get("token");
       boolean result = authService.introspect(token);
       Map<String,Boolean> data = Map.of("active",result);
       return ResponseEntity.ok(ApiResponse.<Map<String,Boolean>>builder()
            .success(true)
            .message("Đã xác minh token")
            .data(data)
            .build());
    }

}
