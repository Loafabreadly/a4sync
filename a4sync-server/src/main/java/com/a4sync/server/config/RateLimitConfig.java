package com.a4sync.server.config;

import io.github.bucket4j.*;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.simple(300, Duration.ofMinutes(1)))  // 300 requests per minute
            .addLimit(Bandwidth.simple(2000, Duration.ofHours(1)))   // 2000 requests per hour
            .build();
    }

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> createBucket());
    }
}
