package com.spkt.libraSys.service.access;

import com.spkt.libraSys.config.CustomUserDetails;
import com.spkt.libraSys.config.JWTUtil;
import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleRepository;
import com.spkt.libraSys.service.token.InvalidatedTokenService;
import com.spkt.libraSys.service.user.*;
import com.spkt.libraSys.service.verificationToken.VerificationService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JWTUtil jwtUtil;
    @Mock
    private InvalidatedTokenService invalidatedTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificationService verificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserEntity testUser;
    private AuthRequest authRequest;
    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = UserEntity.builder()
                .userId("test123")
                .username("testuser")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .isActive(UserStatus.ACTIVE)
                .build();

        // Setup test role
        userRole = RoleEntity.builder()
                .roleName("USER")
                .build();
        testUser.setRoleEntities(Collections.singleton(userRole));

        // Setup auth request
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password");
        authRequest.setConfirmPassword("password");
    }

    @Test
    void login_Success() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(any(UserEntity.class))).thenReturn("test.jwt.token");

        // Act
        AuthResponse response = authService.login(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals("test.jwt.token", response.getToken());
        assertNotNull(response.getUserInfo());
        assertEquals("testuser", response.getUserInfo().getUsername());
        assertEquals("Test User", response.getUserInfo().getFullName());
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> authService.login(authRequest));
    }

    @Test
    void login_InvalidCredentials() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(UserException.class, () -> authService.login(authRequest));
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // Act
        authService.register(authRequest);

        // Assert
        verify(userRepository).save(any(UserEntity.class));
        verify(verificationService).verificationCode(anyString());
    }

    @Test
    void register_UserAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> authService.register(authRequest));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void handleGoogleLogin_NewUser() {
        // Arrange
        String email = "newuser@gmail.com";
        String name = "New User";
        when(userRepository.findByUsername(email)).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(UserEntity.class))).thenReturn("test.jwt.token");

        // Act
        AuthResponse response = authService.handleGoogleLogin(email, name);

        // Assert
        assertNotNull(response);
        assertEquals("test.jwt.token", response.getToken());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void handleGoogleLogin_ExistingUser() {
        // Arrange
        String email = "existinguser@gmail.com";
        when(userRepository.findByUsername(email)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(UserEntity.class))).thenReturn("test.jwt.token");

        // Act
        AuthResponse response = authService.handleGoogleLogin(email, "Existing User");

        // Assert
        assertNotNull(response);
        assertEquals("test.jwt.token", response.getToken());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void logout_Success() {
        // Arrange
        String token = "test.jwt.token";
        when(jwtUtil.extractJti(anyString())).thenReturn("jti123");
        when(jwtUtil.getExpirationTime(anyString())).thenReturn(3600L);

        // Act
        authService.logout(token);

        // Assert
        verify(invalidatedTokenService).saveBlacklistedToken("jti123", 3600L);
    }

    @Test
    void introspect_ValidToken() {
        // Arrange
        String token = "valid.jwt.token";
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 3600000)); // Future date
        when(claims.getSubject()).thenReturn("testuser");
        when(claims.getId()).thenReturn("jti123");
        when(jwtUtil.extractAllClaims(anyString())).thenReturn(claims);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(invalidatedTokenService.isJtiBlacklisted("jti123")).thenReturn(false);

        // Act
        Boolean result = authService.introspect(token);

        // Assert
        assertTrue(result);
        verify(jwtUtil).extractAllClaims(token);
        verify(userRepository).findByUsername("testuser");
        verify(invalidatedTokenService).isJtiBlacklisted("jti123");
    }

    @Test
    void introspect_InvalidToken() {
        // Arrange
        String token = "invalid.jwt.token";
        when(jwtUtil.extractAllClaims(anyString())).thenThrow(new RuntimeException());

        // Act
        Boolean result = authService.introspect(token);

        // Assert
        assertFalse(result);
    }
} 