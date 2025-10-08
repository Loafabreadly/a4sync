package com.a4sync.common.model;

import lombok.Data;

/**
 * Download-specific settings
 */
@Data
public class DownloadSettings {
    private long chunkSize = 1048576; // 1MB
    private int maxParallelDownloads = 4;
    private boolean verifyChecksums = true;
    private boolean enableResume = true;
    private int downloadTimeoutMs = 30000;
}