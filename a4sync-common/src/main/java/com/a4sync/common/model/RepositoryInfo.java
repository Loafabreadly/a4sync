package com.a4sync.common.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepositoryInfo {
    private String name;
    private String description;
    private String version;
    private String url;
    private LocalDateTime lastUpdated;
    private List<ModSet> modSets;
    private int modSetCount;
    private RepositoryConfig config;
    
    @Data
    public static class RepositoryConfig {
        private long maxChunkSize;
        private int parallelDownloads;
        private boolean authenticationRequired;
        private boolean compressionEnabled;
        private String supportedClientVersion;
    }
}