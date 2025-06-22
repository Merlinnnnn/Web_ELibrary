package com.spkt.libraSys.service.user;

import com.spkt.libraSys.exception.ApiResponse;
import com.spkt.libraSys.service.PageDTO;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller managing user-related requests in the system.
 * Provides APIs for creating, updating, and deleting users, as well as account verification and password change functionality.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/users")
public class UserController {

    UserService userService;

    /**
     * Retrieves all users with optional filtering and pagination.
     * Requires ADMIN or MANAGER role.
     *
     * @param username Optional username filter
     * @param role Optional role filter
     * @param pageable Pagination parameters
     * @return Paginated list of users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageDTO<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role
            ,Pageable pageable) {
        PageDTO<UserResponse> response = userService.getAllUsers(username,role, pageable);
        return ResponseEntity.ok(
                ApiResponse.<PageDTO<UserResponse>>builder()
                        .data(response)
                        .message("Lấy danh sách tất cả người dùng thành công")
                        .build()
        );
    }

    /**
     * Retrieves user information by ID.
     *
     * @param userId ID of the user to retrieve
     * @return User information
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId) {
        UserResponse userResponse = userService.getUserById(userId);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .data(userResponse)
                        .message("Lấy thông tin người dùng thành công")
                        .build()
        );
    }

    /**
     * Retrieves information of the currently authenticated user.
     *
     * @return Current user's information
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo() {
        ApiResponse<UserResponse> response =
         ApiResponse.<UserResponse>builder()
                .data(userService.getMyInfo())
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Updates user information.
     *
     * @param userId ID of the user to update
     * @param request Updated user information
     * @return Updated user information
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse updatedUser = userService.updateUser(userId, request);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .data(updatedUser)
                        .message("Cập nhật tài khoản thành công")
                        .build()
        );
    }

    /**
     * Creates a new user.
     *
     * @param request User creation request
     * @return Created user information
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {

        UserResponse userResponse = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED) // 201 Created
                .body(ApiResponse.<UserResponse>builder()
                        .data(userResponse)
                        .message("Tạo tài khoản thành công")
                        .build());
    }

    /**
     * Changes user password.
     *
     * @param request Password change request
     * @return Success response
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Đổi mật khẩu thành công")
                        .build()
        );
    }

    /**
     * Deletes a user by ID.
     * Requires ADMIN role.
     *
     * @param userId ID of the user to delete
     * @return Success response
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId){
        userService.deleteUser(userId);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Người dùng đã được xóa thành công")
                        .build()
        );
    }

    /**
     * Deletes multiple users by their IDs.
     * Requires ADMIN role.
     *
     * @param userIds List of user IDs to delete
     * @return Success response
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUsers(@RequestBody List<String> userIds) {
        userService.deleteUsersByIds(userIds);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Xóa nhiều người dùng thành công")
                        .build()
        );
    }

    /**
     * Locks a user account.
     * Requires ADMIN role.
     *
     * @param userId ID of the user to lock
     * @param reason Optional reason for locking
     * @return Success response
     */
    @PatchMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> lockUser(@PathVariable String userId, @RequestParam(required = false) String reason) {
        userService.lockUser(userId, reason);
        return ApiResponse.<String>builder()
                .data("Người dùng đã bị khóa thành công")
                .build();
    }

    /**
     * Unlocks a user account.
     * Requires ADMIN role.
     *
     * @param userId ID of the user to unlock
     * @return Success response
     */
    @PatchMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> unlockUser(@PathVariable String userId) {
        userService.unlockUser(userId);
        return ApiResponse.<String>builder()
                .data("Người dùng đã được mở khóa thành công")
                .build();
    }
}