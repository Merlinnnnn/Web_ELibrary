package com.spkt.libraSys.service.user;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.email.EmailService;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleRepository;
import com.spkt.libraSys.service.role.RoleService;
import com.spkt.libraSys.service.verificationToken.VerificationService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the UserService interface.
 * Handles user management operations including creation, updates, deletion, and account status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
    AuthService authService;
    UserMapper userMapper;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    RoleService roleService;
    private final EmailService emailService;

    @Override
    public UserResponse getMyInfo() {
        return userMapper.toUserResponse(authService.getCurrentUser());
    }

    @Override
    public UserResponse getUserById(String id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.DUPLICATE_USER, "Username đã tồn tại");
        }

        // Convert DTO to entity
        UserEntity user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Assign default role (USER_ROLE)
        RoleEntity defaultRole = roleRepository.findByRoleName("USER")
                        .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND,"Role error"));
        user.setRoleEntities(Set.of(defaultRole));
        // Set user status to active upon registration
        user.setIsActive(UserStatus.ACTIVE);
        // Save user to database
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public PageDTO<UserResponse> getAllUsers(String username, String role, Pageable pageable) {
        UserEntity userCurr = authService.getCurrentUser();
        boolean isAdmin = userCurr.getRoleEntities().stream()
                .anyMatch(ROLE -> "ADMIN".equals(ROLE.getRoleName()));

        if (!isAdmin) {
            role = "USER";
        }
        Page<UserEntity> users;

        if ((username != null && !username.trim().isEmpty()) && (role != null && !role.trim().isEmpty())) {
            users = userRepository.findByUsernameContainingIgnoreCaseAndRoleEntities_RoleName(username, role, pageable);
        } else if (username != null && !username.trim().isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else if (role != null && !role.trim().isEmpty()) {
            users = userRepository.findByRoleEntities_RoleName(role, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return new PageDTO<>(users.map(userMapper::toUserResponse));
    }

    @Transactional
    @Override
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        // Get current user information from SecurityContext
        UserEntity currentUser = authService.getCurrentUser();

        // Find user to update by ID
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại."));

         if (!currentUser.getUserId().equals(userId)) {
            // If not admin and not the user trying to modify their own information
            throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền thay đổi tài khoản của người khác.");
        }

        // Map information from request to User entity
        userMapper.updateUser(user, request);

        // Save updated user information
        userRepository.save(user);

        // Return response with user information
        return userMapper.toUserResponse(user);
    }

    @Override
    public void deleteUser(String id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsActive(UserStatus.DELETED);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        UserEntity user = authService.getCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_FAILED);
        }
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AppException(ErrorCode.PASSWORD_FAILED,"PASSWORD_CONFIRM_NOT_MATCH");
        }
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    @Override
    public void deleteUsersByIds(List<String> userIds) {
        List<UserEntity> users = userRepository.findAllById(userIds);
        if (users.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        for (UserEntity user : users) {
            user.setIsActive(UserStatus.DELETED);
        }
        userRepository.saveAll(users);
    }

    @Override
    public void lockUser(String userId, String reason) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsActive(UserStatus.LOCKED);
        user.setLockedAt(LocalDateTime.now());
        user.setLockReason(reason);
        userRepository.save(user);

        // Send notification email
        sendLockNotificationEmail(user, reason);
    }

    /**
     * Sends a notification email to a user when their account is locked.
     * @param user The user whose account is locked
     * @param reason The reason for locking the account
     */
    private void sendLockNotificationEmail(UserEntity user, String reason) {
        String toEmail = user.getUsername();
        String subject = "[Thông báo] Tài khoản của bạn đã bị khóa";
        String body = String.format("""
            Kính gửi %s %s,
            
            Tài khoản thư viện của bạn đã bị khóa.
            Lý do: %s
            
            Vui lòng liên hệ quản trị viên để được hỗ trợ mở khóa.
            
            Trân trọng,
            Ban Quản lý Thư viện
            """,
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : "",
                (reason != null && !reason.isBlank()) ? reason : "Không có lý do cụ thể"
        );

       emailService.sendEmailAsync(toEmail, subject, body);
    }

    @Override
    public void unlockUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getIsActive() != UserStatus.LOCKED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tai khoan nguoi dung chua bi khoa.");
        }
        user.setIsActive(UserStatus.ACTIVE);
        user.setLockedAt(null);
        user.setLockReason(null);
        userRepository.save(user);
    }
}