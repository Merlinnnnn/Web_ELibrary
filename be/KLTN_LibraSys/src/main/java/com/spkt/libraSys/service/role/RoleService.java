package com.spkt.libraSys.service.role;

import com.spkt.libraSys.service.user.UserEntity;

import java.util.List;

/**
 * The RoleService interface defines behaviors related to user role management.
 * Provides functionality for role checking, assignment, and administrative role verification.
 */
public interface RoleService {

    /**
     * Checks if a user has a specific role.
     *
     * @param userId ID of the user to check
     * @param roleName Name of the role to verify
     * @return true if the user has the specified role, false otherwise
     */
    boolean hasRole(String userId, String roleName);

    /**
     * Checks if a user is an administrator.
     *
     * @param user User entity to check
     * @return true if the user is an administrator, false otherwise
     */
    Boolean isAdmin(UserEntity user);

    /**
     * Checks if a user is either an administrator or a manager.
     *
     * @param user User entity to check
     * @return true if the user is an administrator or manager, false otherwise
     */
    Boolean isAdminOrManager(UserEntity user);

    /**
     * Assigns a list of roles to a user.
     *
     * @param userId ID of the user to assign roles to
     * @param roles List of role names to assign
     */
    void assignRoleToUser(String userId, List<String> roles);
}