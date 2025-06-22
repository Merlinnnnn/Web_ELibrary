package com.spkt.libraSys.service.token;

/**
 * Service interface for managing invalidated JWT tokens.
 * Provides functionality for tracking and validating blacklisted tokens.
 */
public interface InvalidatedTokenService {
    /**
     * Checks if a JWT token ID (JTI) is blacklisted.
     *
     * @param jti JWT token ID to check
     * @return true if the token is blacklisted, false otherwise
     */
    boolean isJtiBlacklisted(String jti);

    /**
     * Saves a token ID to the blacklist with its expiration time.
     *
     * @param jti JWT token ID to blacklist
     * @param expirationTimeMillis Expiration time of the token in milliseconds
     */
    void saveBlacklistedToken(String jti, long expirationTimeMillis);
}
