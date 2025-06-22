package com.spkt.libraSys.service.user;
import com.spkt.libraSys.service.PageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * The UserService interface defines behaviors related to user management in the system.
 * These methods include creating, updating, deleting, and managing user information.
 */
public interface UserService {

    /**
     * Retrieves the current user's personal information.
     *
     * @return UserResponse containing the current user's information
     */
    UserResponse getMyInfo();

    /**
     * Retrieves user information by ID.
     *
     * @param id ID of the user
     * @return UserResponse containing detailed user information
     */
    UserResponse getUserById(String id);

    /**
     * Creates a new user in the system.
     *
     * @param userRequest Data for creating a new user
     * @return UserResponse containing the information of the newly created user
     */
    UserResponse createUser(UserRequest userRequest);

    /**
     * Retrieves a list of all users with pagination and username search support.
     *
     * @param username Username to search for (optional)
     * @param role Role to filter by (optional)
     * @param pageable Pagination information
     * @return PageDTO containing the list of users and pagination information
     */
    PageDTO<UserResponse> getAllUsers(String username, String role, Pageable pageable);

    /**
     * Updates user information.
     *
     * @param id ID of the user to update
     * @param userRequest New data to update user information
     * @return UserResponse containing the updated user information
     */
    UserResponse updateUser(String id, UserUpdateRequest userRequest);

    /**
     * Deletes a user by ID.
     *
     * @param id ID of the user to delete
     */
    void deleteUser(String id);

    /**
     * Changes the user's password.
     *
     * @param request Contains password change request information
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * Deletes multiple users by their IDs.
     *
     * @param userIds List of user IDs to delete
     */
    void deleteUsersByIds(List<String> userIds);

    /**
     * Locks a user account with a specific reason.
     *
     * @param userId ID of the user to lock
     * @param reason Reason for locking the account
     */
    void lockUser(String userId, String reason);

    /**
     * Unlocks a user account.
     *
     * @param userId ID of the user to unlock
     */
    void unlockUser(String userId);
}