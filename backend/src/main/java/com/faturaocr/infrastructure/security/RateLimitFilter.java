package com.faturaocr.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitFilter.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.public-limit:20}")
    private int publicLimit;

    @Value("${app.security.rate-limit.login-limit:5}")
    private int loginLimit;

    @Value("${app.security.rate-limit.authenticated-limit:100}")
    private int authenticatedLimit;

    @Value("${app.security.rate-limit.upload-limit:10}")
    private int uploadLimit;

    @Value("${app.security.rate-limit.export-limit:5}")
    private int exportLimit;

    @Value("${app.security.rate-limit.admin-limit:50}")
    private int adminLimit;

    @Value("${app.security.rate-limit.window-seconds:60}")
    private int windowSeconds;

    public RateLimitFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip WebSocket
        if (request.getRequestURI().startsWith("/ws")) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitTier tier = resolveTier(request);
        long limit = getLimitForTier(tier);

        String clientKey = resolveClientKey(request, tier);
        String redisKey = "rate_limit:" + tier.name() + ":" + clientKey;

        try {
            long now = Instant.now().getEpochSecond();
            long windowStart = now - windowSeconds;

            // Using ZSET for sliding window
            ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();

            // Execute in transaction/pipeline for atomicity could be better, but basic ZSET
            // ops are redundant safe enough for this scale
            // Remove old entries
            zSet.removeRangeByScore(redisKey, 0, windowStart);

            // Count current
            Long count = zSet.zCard(redisKey);
            long currentCount = count != null ? count : 0;

            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount - 1)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(now + windowSeconds));

            if (currentCount >= limit) {
                handleRateLimitExceeded(response, windowSeconds);
                return;
            }

            // Add current request
            zSet.add(redisKey, String.valueOf(now) + "-" + System.nanoTime(), now);
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Fail open if Redis is down
            LOGGER.error("Rate limit check failed", e);
            filterChain.doFilter(request, response);
        }
    }

    private void handleRateLimitExceeded(HttpServletResponse response, long retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("error", "RATE_LIMIT_EXCEEDED");
        error.put("message", "İstek limiti aşıldı. Lütfen " + retryAfter + " saniye sonra tekrar deneyin.");
        error.put("retryAfter", retryAfter);

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private RateLimitTier resolveTier(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/api/v1/auth/login") && "POST".equalsIgnoreCase(method)) {
            return RateLimitTier.LOGIN;
        }
        if (path.startsWith("/api/v1/auth")) {
            return RateLimitTier.PUBLIC;
        }
        if (path.startsWith("/api/v1/admin")) {
            return RateLimitTier.ADMIN;
        }
        if ((path.startsWith("/api/v1/invoices/upload") || path.startsWith("/api/v1/invoices/bulk-upload"))
                && "POST".equalsIgnoreCase(method)) {
            return RateLimitTier.UPLOAD;
        }
        if (path.startsWith("/api/v1/invoices/export")) {
            return RateLimitTier.EXPORT;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return RateLimitTier.AUTHENTICATED;
        }

        return RateLimitTier.PUBLIC;
    }

    private long getLimitForTier(RateLimitTier tier) {
        return switch (tier) {
            case PUBLIC -> publicLimit;
            case LOGIN -> loginLimit;
            case AUTHENTICATED -> authenticatedLimit;
            case UPLOAD -> uploadLimit;
            case EXPORT -> exportLimit;
            case ADMIN -> adminLimit;
        };
    }

    private String resolveClientKey(HttpServletRequest request, RateLimitTier tier) {
        if (tier == RateLimitTier.PUBLIC || tier == RateLimitTier.LOGIN) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId().toString();
        }
        return request.getRemoteAddr(); // Fallback
    }

    public enum RateLimitTier {
        PUBLIC,
        LOGIN,
        AUTHENTICATED,
        UPLOAD,
        EXPORT,
        ADMIN
    }
}
