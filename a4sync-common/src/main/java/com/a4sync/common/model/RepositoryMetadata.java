package com.a4sync.common.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository metadata configuration
 */
@Data
public class RepositoryMetadata {
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