package com.a4sync.server.security;

import com.a4sync.server.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = resolveClientId(request);
        Bucket bucket = rateLimitConfig.resolveBucket(clientId);
        
        if (bucket.tryConsume(1)) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After", "60");
            response.getWriter().write("Too many requests - please try again later");
        }
    }

    private String resolveClientId(HttpServletRequest request) {
        // Try to get client IP, fallback to session ID or generate a unique ID
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
}
