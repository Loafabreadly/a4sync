package com.a4sync.common.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * A4Sync repository configuration file format (.a4sync)
 * This is our custom format for easy repository setup and auto-configuration
 */
@Data
public class A4SyncConfig {
    private String formatVersion = "1.0";
    private RepositoryMetadata repository;
    private ConnectionSettings connection;
    private ClientSettings client;
    
    @Data
    public static class RepositoryMetadata {
        private String name;
        private String description;
        private String version;
        private String maintainer;
        private String contactEmail;
        private String website;
        private LocalDateTime lastUpdated;
        private List<String> supportedGames; // ["arma3", "arma4"]
        private Map<String, String> customProperties;
    }
    
    @Data
    public static class ConnectionSettings {
        private String baseUrl;
        private boolean requiresAuthentication;
        private String authenticationMethod; // "basic", "token", "none"
        private List<String> mirrorUrls;
        private long timeoutMs = 30000;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
    }
    
    @Data
    public static class ClientSettings {
        private DownloadSettings download;
        private SecuritySettings security;
        private UISettings ui;
        
        @Data
        public static class DownloadSettings {
            private long maxChunkSize = 52428800L; // 50MB
            private int maxParallelDownloads = 3;
            private long bandwidthLimitBps = 0; // 0 = unlimited
            private boolean resumeSupported = true;
            private boolean compressionEnabled = true;
            private String checksumAlgorithm = "SHA-256";
        }
        
        @Data
        public static class SecuritySettings {
            private boolean verifySignatures = true;
            private boolean allowUnsignedMods = false;
            private List<String> trustedKeyServers;
            private boolean checkCertificates = true;
        }
        
        @Data
        public static class UISettings {
            private String theme = "default";
            private boolean showAdvancedOptions = false;
            private boolean autoRefreshEnabled = true;
            private long autoRefreshIntervalMs = 300000; // 5 minutes
            private Map<String, String> customizations;
        }
    }
    
    /**
     * Creates a default A4Sync configuration for a repository
     */
    public static A4SyncConfig createDefault(String repositoryName, String baseUrl) {
        A4SyncConfig config = new A4SyncConfig();
        
        // Repository metadata
        RepositoryMetadata metadata = new RepositoryMetadata();
        metadata.setName(repositoryName);
        metadata.setDescription("A4Sync Repository");
        metadata.setVersion("1.0");
        metadata.setLastUpdated(LocalDateTime.now());
        metadata.setSupportedGames(List.of("arma3", "arma4"));
        config.setRepository(metadata);
        
        // Connection settings
        ConnectionSettings connection = new ConnectionSettings();
        connection.setBaseUrl(baseUrl);
        connection.setRequiresAuthentication(false);
        connection.setAuthenticationMethod("none");
        config.setConnection(connection);
        
        // Client settings
        ClientSettings clientSettings = new ClientSettings();
        clientSettings.setDownload(new ClientSettings.DownloadSettings());
        clientSettings.setSecurity(new ClientSettings.SecuritySettings());
        clientSettings.setUi(new ClientSettings.UISettings());
        config.setClient(clientSettings);
        
        return config;
    }
}