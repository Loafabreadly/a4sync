package com.a4sync.common.model;

import lombok.Data;

/**
 * Repository-specific configuration settings
 */
@Data
public class RepositoryConfig {
    private long maxChunkSize;
    private int parallelDownloads;
    private boolean authenticationRequired;
    private boolean compressionEnabled;
    private String supportedClientVersion;
}