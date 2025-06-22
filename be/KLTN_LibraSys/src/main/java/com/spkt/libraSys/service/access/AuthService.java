package com.spkt.libraSys.service.access;

import com.spkt.libraSys.service.user.UserEntity;

import java.text.ParseException;

/**
 * Interface providing methods for user authentication,
 * including login, logout, token validation, and token refresh functionality.
 */
public interface AuthService {

    /**
     * Authenticates a user and performs login.
     *
     * @param request Login request containing username and password
     * @return Authentication response containing access token, refresh token, and user information
     */
    AuthResponse login(AuthRequest request);

    /**
     * Logs out a user by invalidating their session.
     *
     * @param token The authentication token to invalidate
     */
    void logout(String token);

    /**
     * Retrieves the currently authenticated user.
     *
     * @return The current user entity
     */
    UserEntity getCurrentUser();

    /**
     * Validates and introspects an OAuth2 token.
     *
     * @param token The token to validate
     * @return Boolean indicating whether the token is valid
     */
    Boolean introspect(String token);

    /**
     * Refreshes an expired access token using a refresh token.
     *
     * @param token The refresh token
     * @return New authentication response with fresh tokens
     */
    AuthResponse refresh(String token);

    /**
     * Registers a new user in the system.
     *
     * @param request Registration request containing user details
     */
    void register(AuthRequest request);

    /**
     * Handles user authentication through Google OAuth.
     *
     * @param email User's email from Google
     * @param name User's name from Google
     * @return Authentication response after successful Google authentication
     */
    AuthResponse handleGoogleLogin(String email, String name);
}
