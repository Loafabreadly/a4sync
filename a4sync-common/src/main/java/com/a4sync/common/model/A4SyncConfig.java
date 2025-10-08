package com.a4sync.common.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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
        clientSettings.setDownload(new DownloadSettings());
        clientSettings.setSecurity(new SecuritySettings());
        clientSettings.setUi(new UISettings());
        config.setClient(clientSettings);
        
        return config;
    }
}