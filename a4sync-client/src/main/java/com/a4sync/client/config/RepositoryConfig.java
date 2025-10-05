package com.a4sync.client.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.nio.file.Path;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryConfig {
    private String name;
    private String url;
    private String password;
    private boolean enabled;
    private boolean checkOnStartup;
    private Path localPath;
    private Instant lastChecked;
    private String lastError;
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isCheckOnStartup() { return checkOnStartup; }
    public void setCheckOnStartup(boolean checkOnStartup) { this.checkOnStartup = checkOnStartup; }
    
    public Path getLocalPath() { return localPath; }
    public void setLocalPath(Path localPath) { this.localPath = localPath; }
    
    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant lastChecked) { this.lastChecked = lastChecked; }
    
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}