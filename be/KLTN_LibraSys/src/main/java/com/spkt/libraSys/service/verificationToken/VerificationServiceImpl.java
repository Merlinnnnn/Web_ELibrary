package com.spkt.libraSys.service.verificationToken;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.VerificationResponse;
import com.spkt.libraSys.service.email.EmailService;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.user.UserStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VerificationServiceImpl implements VerificationService {
    VerificationTokenRepository verificationTokenRepository;
    UserRepository userRepository;
    EmailService emailService;
    PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    @Value("${my-config.base-url}")
    @NonFinal
    String baseUrl;
    @Value("${my-config.client-url}")
    @NonFinal
    String clientUrl;

    private final int TIMEOUT_IN_MINUTES = 10;


    @Override
    public VerificationResponse verifyAccount(String token) {
        log.info("Verify token email: {}", token);
        VerificationTokenEntity verificationToken = verificationTokenRepository.findByToken(token);

        if (verificationToken == null || verificationToken.getExpiryDate().isBefore(LocalDateTime.now())
                || verificationToken.getType() != VerificationTokenEntity.TokenType.VERIFY_EMAIL) {
            return VerificationResponse.builder().verified(false).build();
        }

        UserEntity user = userRepository.findByUsername(verificationToken.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + verificationToken.getEmail()));

        user.setIsActive(UserStatus.ACTIVE);
        userRepository.save(user);
        roleService.assignRoleToUser(user.getUserId(),List.of("USER"));

        verificationTokenRepository.delete(verificationToken); // Xoá token sau khi sử dụng

        return VerificationResponse.builder()
                .verified(true)
                .email(user.getUsername())
                .verifiedAt(LocalDateTime.now().toString())
                .build();
    }

    @Transactional
    @Override
    public void verificationCode(String email) {
        // Xoá tất cả các token xác minh cũ
        verificationTokenRepository.deleteAllByEmailAndType(email, VerificationTokenEntity.TokenType.VERIFY_EMAIL);

        String token = UUID.randomUUID().toString();
        VerificationTokenEntity newToken = VerificationTokenEntity.builder()
                .email(email)
                .token(token)
                .type(VerificationTokenEntity.TokenType.VERIFY_EMAIL)
                .expiryDate(LocalDateTime.now().plusMinutes(TIMEOUT_IN_MINUTES))
                .build();

        verificationTokenRepository.save(newToken);

        String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        String subject = "Xác minh tài khoản của bạn";
        String body = "Chào bạn,\n\nĐây là liên kết xác minh tài khoản của bạn: " + verificationUrl +
                "\nLiên kết sẽ hết hạn sau 10 phút.\n\nTrân trọng,\nĐội ngũ hỗ trợ.";

        emailService.sendEmailAsync(email, subject, body);
    }

    @Transactional
    @Override
    public boolean resendVerificationCode(String email) {
        UserEntity user = userRepository.findByUsername(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getIsActive() != UserStatus.PENDING) {
            return false; // Không gửi lại mã nếu đã kích hoạt
        }

        verificationCode(email);
        return true;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<UserEntity> userOptional = userRepository.findByUsername(email);
        if (userOptional.isEmpty()) {
            log.warn("No user found with email: " + email);
            throw new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + email);
        }

        // Xoá token reset mật khẩu cũ
        verificationTokenRepository.deleteAllByEmailAndType(email, VerificationTokenEntity.TokenType.RESET_PASSWORD);

        String resetToken = UUID.randomUUID().toString();
        VerificationTokenEntity newResetToken = VerificationTokenEntity.builder()
                .email(email)
                .token(resetToken)
                .type(VerificationTokenEntity.TokenType.RESET_PASSWORD)
                .expiryDate(LocalDateTime.now().plusMinutes(TIMEOUT_IN_MINUTES))
                .build();

        verificationTokenRepository.save(newResetToken);

        String resetUrl = clientUrl + "/auth/reset-password?token=" + resetToken;
        String subject = "Yêu cầu đặt lại mật khẩu";
        String body = "Chào bạn,\n\nĐây là liên kết để đặt lại mật khẩu của bạn: " + resetUrl +
                "\nLiên kết sẽ hết hạn sau 15 phút.\n\nTrân trọng,\nĐội ngũ hỗ trợ.";

        emailService.sendEmailAsync(email, subject, body);
    }

    @Override
    public boolean resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        VerificationTokenEntity tokenEntity = verificationTokenRepository.findByToken(request.getToken());
        if (tokenEntity == null || tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())
                || tokenEntity.getType() != VerificationTokenEntity.TokenType.RESET_PASSWORD) {
            return false;
        }

        Optional<UserEntity> userOptional = userRepository.findByUsername(tokenEntity.getEmail());
        if (userOptional.isEmpty()) return false;

        UserEntity user = userOptional.get();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        verificationTokenRepository.delete(tokenEntity); // Xoá token sau khi reset

        return true;
    }
}
