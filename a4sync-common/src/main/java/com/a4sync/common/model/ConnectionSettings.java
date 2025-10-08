package com.a4sync.common.model;

import lombok.Data;
import java.util.List;

/**
 * Connection settings configuration
 */
@Data
public class ConnectionSettings {
    private String baseUrl;
    private boolean requiresAuthentication;
    private String authenticationMethod; // "basic", "token", "none"
    private List<String> mirrorUrls;
    private long timeoutMs = 30000;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
}