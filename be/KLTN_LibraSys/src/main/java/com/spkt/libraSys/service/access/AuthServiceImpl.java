package com.spkt.libraSys.service.access;

import com.spkt.libraSys.config.CustomUserDetails;
import com.spkt.libraSys.config.JWTUtil;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.email.EmailService;
import com.spkt.libraSys.service.token.InvalidatedToken;
import com.spkt.libraSys.service.token.InvalidatedTokenRepository;
import com.spkt.libraSys.service.token.InvalidatedTokenService;
import com.spkt.libraSys.service.user.*;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleRepository;
import com.spkt.libraSys.service.verificationToken.VerificationService;
import com.spkt.libraSys.service.verificationToken.VerificationTokenEntity;
import com.spkt.libraSys.service.verificationToken.VerificationTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the AuthService interface providing authentication and authorization functionality.
 * Handles user login, registration, token management, and Google OAuth integration.
 */
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class AuthServiceImpl implements AuthService {
    AuthenticationManager authManager;
    UserRepository userRepository;
    RoleRepository roleRepository;
    JWTUtil jwtUtil;
    InvalidatedTokenService invalidatedTokenService;
    PasswordEncoder passwordEncoder;
    VerificationService verificationService;
    @Value("${my-config.base-url}")
    @NonFinal
    private String baseUrl;

    @Override
    public AuthResponse login(AuthRequest request) {
        UserEntity userEntity = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            throw new UserException(ErrorCode.INVALID_CREDENTIALS);
        }

        validateUserStatus(userEntity);
        return buildLoginResponse(userEntity);
    }

    @Override
    public void logout(String token) {
        try {
            String jti = jwtUtil.extractJti(token);
            long expirationTime = jwtUtil.getExpirationTime(token);
            invalidatedTokenService.saveBlacklistedToken(jti, expirationTime);
        } catch (Exception e) {
            // Log the exception if needed
        }
    }

    @Override
    public UserEntity getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            String userId = ((CustomUserDetails) principal).getUserId();
            return userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy User đang đăng nhập"));
        }
        throw new AppException(ErrorCode.UNAUTHORIZED, "Người dùng chưa đăng nhập");
    }

    @Override
    public Boolean introspect(String token) {
        try {
            Claims claims = jwtUtil.extractAllClaims(token);
            if (claims.getExpiration().before(new Date()))
                return false;
            String username = claims.getSubject();
            Optional<UserEntity> userEntity = userRepository.findByUsername(username);
            if(userEntity.isEmpty())
                return false;
            UserEntity user = userEntity.get();
            return user.getIsActive().equals(UserStatus.ACTIVE) && !invalidatedTokenService.isJtiBlacklisted(claims.getId());
        } catch (Exception e) {
           return false;
        }
    }

    @Override
    public AuthResponse refresh(String token) {
        // Verify and decode refresh token
        String username = verifyToken(token, true);  // true indicates this is a refresh token

        String jit = jwtUtil.extractJti(token);
        long expiryTime = jwtUtil.getExpirationTime(token);

        invalidatedTokenService.saveBlacklistedToken(jit, expiryTime);

        // Generate new access token for the user
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        String newToken = jwtUtil.generateToken(userEntity);  // Generate new access token
        UserInfo userInfo = UserInfo.builder()
                .userId(userEntity.getUserId())
                .username(userEntity.getUsername())
                .firstName(userEntity.getFirstName())
                .lastName(userEntity.getLastName())
                .fullName(userEntity.getFirstName() + " " + userEntity.getLastName())
                .roles(userEntity.getRoleEntities().stream()
                        .map(role -> role.getRoleName())
                        .collect(Collectors.toList()))
                .build();

        return AuthResponse.builder()
                .token(newToken)
                .userInfo(userInfo)
                .build();
    }

    private String verifyToken(String token, boolean isRefresh) {
        // Decode and verify token
        Claims claims = jwtUtil.extractAllClaims(token);

        // Check token expiration time
        Date expiryTime = claims.getExpiration();
        if (expiryTime.before(new Date())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Check if refresh token has been revoked (stored in DB or cache)
        String jti = claims.getId();  // JWT ID
        if (invalidatedTokenService.isJtiBlacklisted(jti)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        // Return username from claim
        return claims.getSubject();
    }

    @Override
    @Transactional
    public void register(AuthRequest request) {
        log.info("Processing registration for username: {}", request.getUsername());
        
        // 1. Validate request
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên đăng nhập không được để trống");
        }
        
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu phải có ít nhất 6 ký tự");
        }
        
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu xác nhận không khớp");
        }
        
        // 2. Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Tên đăng nhập đã tồn tại");
        }
        
        // 3. Encode password
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        // 4. Create new user
        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .isActive(UserStatus.PENDING)
                .build();
                
        // 5. Save user
        userRepository.save(user);
        verificationService.verificationCode(user.getUsername());
        log.info("User registered successfully: {}", user.getUsername());
    }

    @Override
    public AuthResponse handleGoogleLogin(String email, String name) {
        // Check if user exists
        UserEntity userEntity = userRepository.findByUsername(email)
                .orElseGet(() -> {
                    // Create new user if doesn't exist
                    UserEntity newUser = new UserEntity();
                    newUser.setUsername(email);
                    newUser.setFirstName(name);
                    newUser.setLastName("");
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setIsActive(UserStatus.ACTIVE);

                    // Set default role (you might want to adjust this)
                    RoleEntity userRole = roleRepository.findByRoleName("USER")
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    newUser.setRoleEntities(Collections.singleton(userRole));

                    return userRepository.save(newUser);
                });

        validateUserStatus(userEntity);
        return buildLoginResponse(userEntity);
    }

    private void validateUserStatus(UserEntity userEntity) {
        switch (userEntity.getIsActive()) {
            case PENDING:
                throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS, "Người dùng chưa được xác minh");
            case LOCKED:
                throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS, "Tài khoản bị khóa");
            case DELETED:
                throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS, "Tài khoản đã bị xóa");
            default:
                // Valid status, do nothing
                break;
        }
    }

    private AuthResponse buildLoginResponse(UserEntity userEntity) {
        // Generate JWT token
        String token = jwtUtil.generateToken(userEntity);

        // Create UserInfo
        UserInfo userInfo = UserInfo.builder()
                .userId(userEntity.getUserId())
                .username(userEntity.getUsername())
                .firstName(userEntity.getFirstName())
                .lastName(userEntity.getLastName())
                .fullName(userEntity.getFirstName() + " " + userEntity.getLastName())
                .roles(userEntity.getRoleEntities().stream()
                        .map(RoleEntity::getRoleName)
                        .collect(Collectors.toList()))
                .build();

        return AuthResponse.builder()
                .token(token)
                .userInfo(userInfo)
                .build();
    }
}
