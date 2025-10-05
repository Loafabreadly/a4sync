package com.a4sync.server.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public CaffeineProxyManager<String> buckets() {
        return new CaffeineProxyManager<>(Caffeine.newBuilder().maximumSize(100000).build());
    }

    @Bean
    public BucketConfiguration bucketConfiguration() {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1)))) // 300 requests per minute
            .addLimit(Bandwidth.classic(2000, Refill.intervally(2000, Duration.ofHours(1)))) // 2000 requests per hour
            .build();
    }

    public Bucket resolveBucket(String key) {
        return buckets().builder().build(key, bucketConfiguration());
    }
}
