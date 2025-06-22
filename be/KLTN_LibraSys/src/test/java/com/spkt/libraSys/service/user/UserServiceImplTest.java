package com.spkt.libraSys.service.user;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.email.EmailService;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleRepository;
import com.spkt.libraSys.service.role.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity testUser;
    private UserResponse testUserResponse;
    private RoleEntity userRole;
    private UserRequest testUserRequest;
    private UserUpdateRequest testUserUpdateRequest;
    private ChangePasswordRequest testChangePasswordRequest;

    @BeforeEach
    void setUp() {
        // Setup test role
        userRole = RoleEntity.builder()
                .roleName("USER")
                .build();

        // Setup test user
        testUser = UserEntity.builder()
                .userId("test123")
                .username("testuser@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .isActive(UserStatus.ACTIVE)
                .roleEntities(Set.of(userRole))
                .build();

        // Setup test user response
        testUserResponse = UserResponse.builder()
                .userId("test123")
                .username("testuser@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(UserStatus.ACTIVE)
                .roles(List.of("USER"))
                .build();

        // Setup test user request
        testUserRequest = new UserRequest();
        testUserRequest.setUsername("newuser@example.com");
        testUserRequest.setPassword("password123");
        testUserRequest.setFirstName("New");
        testUserRequest.setLastName("User");

        // Setup test user update request
        testUserUpdateRequest = new UserUpdateRequest();
        testUserUpdateRequest.setFirstName("Updated");
        testUserUpdateRequest.setLastName("Name");

        // Setup test change password request
        testChangePasswordRequest = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .confirmNewPassword("newPassword")
                .build();
    }

    @Test
    void getMyInfo_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        // Act
        UserResponse response = userService.getMyInfo();

        // Assert
        assertNotNull(response);
        assertEquals(testUserResponse.getUserId(), response.getUserId());
        assertEquals(testUserResponse.getUsername(), response.getUsername());
        verify(authService).getCurrentUser();
        verify(userMapper).toUserResponse(testUser);
    }

    @Test
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));
        when(userMapper.toUserResponse(testUser)).thenReturn(testUserResponse);

        // Act
        UserResponse response = userService.getUserById("test123");

        // Assert
        assertNotNull(response);
        assertEquals(testUserResponse.getUserId(), response.getUserId());
        verify(userRepository).findById("test123");
    }

    @Test
    void getUserById_UserNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> userService.getUserById("nonexistent"));
    }

    @Test
    void createUser_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userMapper.toUser(any(UserRequest.class))).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(userMapper.toUserResponse(any(UserEntity.class))).thenReturn(testUserResponse);

        // Act
        UserResponse response = userService.createUser(testUserRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUserResponse.getUserId(), response.getUserId());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void createUser_DuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(AppException.class, () -> userService.createUser(testUserRequest));
    }

    @Test
    void getAllUsers_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<UserEntity> users = Collections.singletonList(testUser);
        Page<UserEntity> userPage = new PageImpl<>(users, pageable, users.size());
        
        // Mock admin user
        UserEntity adminUser = UserEntity.builder()
                .userId("admin123")
                .username("admin@example.com")
                .roleEntities(Set.of(RoleEntity.builder().roleName("ADMIN").build()))
                .build();
        
        when(authService.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toUserResponse(any(UserEntity.class))).thenReturn(testUserResponse);

        // Act
        PageDTO<UserResponse> response = userService.getAllUsers(null, null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void updateUser_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);
        when(userMapper.toUserResponse(any(UserEntity.class))).thenReturn(testUserResponse);

        // Act
        UserResponse response = userService.updateUser("test123", testUserUpdateRequest);

        // Assert
        assertNotNull(response);
        verify(userMapper).updateUser(any(UserEntity.class), any(UserUpdateRequest.class));
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void updateUser_Unauthorized() {
        // Arrange
        UserEntity otherUser = UserEntity.builder()
                .userId("other123")
                .username("other@example.com")
                .build();
        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(AppException.class, () -> userService.updateUser("test123", testUserUpdateRequest));
    }

    @Test
    void changePassword_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");

        // Act
        userService.changePassword(testChangePasswordRequest);

        // Assert
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void changePassword_WrongOldPassword() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(AppException.class, () -> userService.changePassword(testChangePasswordRequest));
    }

    @Test
    void lockUser_Success() {
        // Arrange
        String reason = "Test lock reason";
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));

        // Act
        userService.lockUser("test123", reason);

        // Assert
        verify(userRepository).save(any(UserEntity.class));
        verify(emailService).sendEmailAsync(anyString(), anyString(), anyString());
    }

    @Test
    void unlockUser_Success() {
        // Arrange
        testUser.setIsActive(UserStatus.LOCKED);
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));

        // Act
        userService.unlockUser("test123");

        // Assert
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void unlockUser_NotLocked() {
        // Arrange
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(AppException.class, () -> userService.unlockUser("test123"));
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.findById("test123")).thenReturn(Optional.of(testUser));

        // Act
        userService.deleteUser("test123");

        // Assert
        assertEquals(UserStatus.DELETED, testUser.getIsActive());
        verify(userRepository).findById("test123");
        // Note: The implementation doesn't call save() after setting the status
    }

    @Test
    void deleteUsersByIds_Success() {
        // Arrange
        List<String> userIds = Arrays.asList("test123", "test456");
        List<UserEntity> users = Arrays.asList(testUser, testUser);
        when(userRepository.findAllById(userIds)).thenReturn(users);
        when(userRepository.saveAll(anyList())).thenReturn(users);

        // Act
        userService.deleteUsersByIds(userIds);

        // Assert
        users.forEach(user -> assertEquals(UserStatus.DELETED, user.getIsActive()));
        verify(userRepository).findAllById(userIds);
        verify(userRepository).saveAll(users);
    }
} 