package com.faturaocr.infrastructure.security;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.infrastructure.common.config.JwtProperties;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Service for managing refresh tokens with Redis.
 */
@Service
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Create a new refresh token for user.
     */
    public String createRefreshToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and User ID must not be null");
        }
        // Generate random token
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Hash token for storage
        String tokenHash = hashToken(token);

        // Store in Redis: token_hash -> user_id
        String key = REFRESH_TOKEN_PREFIX + tokenHash;

        if (jwtProperties.getRefreshToken() == null) {
            throw new IllegalStateException("JWT Refresh Token properties are not configured");
        }

        Duration expiration = Duration.ofMillis(jwtProperties.getRefreshToken().getExpiration());

        String userIdStr = user.getId().toString();
        redisTemplate.opsForValue().set(key, userIdStr, expiration);

        // Add to user's token set
        redisTemplate.opsForSet().add(USER_TOKENS_PREFIX + userIdStr, tokenHash);
        redisTemplate.expire(USER_TOKENS_PREFIX + userIdStr, expiration); // Refresh set expiry too? Ideally max of
                                                                          // valid tokens. Simplification: Refresh
                                                                          // expiry on add.

        return token;
    }

    /**
     * Validate refresh token.
     */
    public boolean validateRefreshToken(String token) {
        String tokenHash = hashToken(token);

        // Check if token is blacklisted
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            return false;
        }

        // Check if token exists
        String key = REFRESH_TOKEN_PREFIX + tokenHash;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get user ID from refresh token.
     */
    public String getUserIdFromToken(String token) {
        String tokenHash = hashToken(token);
        String key = REFRESH_TOKEN_PREFIX + tokenHash;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Revoke refresh token (add to blacklist).
     */
    public void revokeRefreshToken(String token) {
        String tokenHash = hashToken(token);

        // Remove from active tokens
        String key = REFRESH_TOKEN_PREFIX + tokenHash;
        String userId = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);

        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + userId, tokenHash);
        }

        // Add to blacklist (with same expiration as original token)
        String blacklistKey = BLACKLIST_PREFIX + tokenHash;

        if (jwtProperties.getRefreshToken() == null) {
            throw new IllegalStateException("JWT Refresh Token properties are not configured");
        }

        Duration expiration = Duration.ofMillis(jwtProperties.getRefreshToken().getExpiration());
        redisTemplate.opsForValue().set(blacklistKey, "revoked", expiration);
    }

    /**
     * Revoke all refresh tokens for a user.
     */
    public void revokeAllUserTokens(String userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        java.util.Set<String> tokenHashes = redisTemplate.opsForSet().members(userTokensKey);

        if (tokenHashes != null && !tokenHashes.isEmpty()) {
            for (String hash : tokenHashes) {
                // Delete active token
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + hash);
                // Assume blacklist is optional for bulk revoke if we just delete them?
                // But validation checks blacklist. If deleted, it returns false (invalid).
                // So deleting is enough to invalidate.
            }
        }
        redisTemplate.delete(userTokensKey);
    }

    /**
     * Hash token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
