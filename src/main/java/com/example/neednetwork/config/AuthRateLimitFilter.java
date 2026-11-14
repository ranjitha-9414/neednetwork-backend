
package com.example.neednetwork.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, RateLimitBucket> buckets =
            new ConcurrentHashMap<>();

    // Rate limit configuration
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private static final int LOGIN_LIMIT = 5;
    private static final int PASSWORD_RESET_LIMIT = 3;
    private static final int VERIFICATION_LIMIT = 3;
    private static final int REGISTER_LIMIT = 5;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();

        return !path.equals("/api/auth/login")
                && !path.equals("/api/auth/forgot-password")
                && !path.equals("/api/auth/resend-verification")
                && !path.equals("/api/auth/register")
                && !path.equals("/api/auth/reset-password");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String clientIp = request.getRemoteAddr();

        int limit = getLimit(path);

        String key = clientIp + ":" + path;

        long now = System.currentTimeMillis();

        RateLimitBucket bucket = buckets.compute(
                key,
                (k, existing) -> {

                    if (existing == null ||
                            now - existing.windowStart >= WINDOW_MS) {

                        return new RateLimitBucket(now);
                    }

                    return existing;
                }
        );

        synchronized (bucket) {

            if (bucket.count >= limit) {

                long remainingTime =
                        WINDOW_MS - (now - bucket.windowStart);

                long retryAfterSeconds =
                        Math.max(1, (remainingTime + 999) / 1000);

                response.setStatus(429);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                response.setHeader(
                        "Retry-After",
                        String.valueOf(retryAfterSeconds)
                );

                response.getWriter().write(
                        "{\"message\":\"Too many requests. Please try again later.\"}"
                );

                return;
            }

            bucket.count++;
        }

        filterChain.doFilter(request, response);
    }

    private int getLimit(String path) {

        return switch (path) {

            case "/api/auth/login" -> LOGIN_LIMIT;

            case "/api/auth/forgot-password",
                 "/api/auth/reset-password" -> PASSWORD_RESET_LIMIT;

            case "/api/auth/resend-verification" -> VERIFICATION_LIMIT;

            case "/api/auth/register" -> REGISTER_LIMIT;

            default -> Integer.MAX_VALUE;
        };
    }

    private static class RateLimitBucket {

        private final long windowStart;
        private int count;

        private RateLimitBucket(long windowStart) {
            this.windowStart = windowStart;
            this.count = 0;
        }
    }
}
