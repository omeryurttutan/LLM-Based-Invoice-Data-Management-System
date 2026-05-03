package com.faturaocr.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginAttemptService {

    private static final String PREFIX = "login_attempts:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void loginSucceeded(String key) {
        redisTemplate.delete(PREFIX + key);
    }

    public void loginFailed(@org.springframework.lang.NonNull String key) {
        String redisKey = PREFIX + key;
        String attempts = redisTemplate.opsForValue().get(redisKey);
        int attemptsCount = attempts == null ? 0 : Integer.parseInt(attempts);
        attemptsCount++;

        redisTemplate.opsForValue().set(redisKey, String.valueOf(attemptsCount),
                Duration.ofMinutes(lockoutDurationMinutes));
    }

    public boolean isBlocked(String key) {
        String redisKey = PREFIX + key;
        String attempts = redisTemplate.opsForValue().get(redisKey);
        return attempts != null && Integer.parseInt(attempts) >= maxAttempts;
    }

    public long getRemainingBlockMinutes(String key) {
        String redisKey = PREFIX + key;
        Long expire = redisTemplate.getExpire(redisKey);
        if (expire == null || expire < 0)
            return 0;
        return (long) Math.ceil(expire / 60.0);
    }

    public void unblock(String key) {
        redisTemplate.delete(PREFIX + key);
    }
}
