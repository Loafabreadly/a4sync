package com.a4sync.client.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Repository {
    private String id;
    private String name;
    private String url;
    private String password;
    private boolean useAuthentication;
    private boolean enabled = true;
    private boolean autoCheck = true;
    private String notes;
    
    // Status and monitoring fields
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;
    private LocalDateTime lastChecked;
    private String lastError;
    private long totalSize;
    private int modCount;
    private int modSetCount;
    
    public Repository(String name, String url) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.url = url;
        this.useAuthentication = false;
        this.autoCheck = true;
    }
    
    public String getDisplayName() {
        return name != null ? name : url;
    }
    
    public boolean hasCredentials() {
        return password != null && !password.trim().isEmpty();
    }
    
    public void updateHealthStatus(HealthStatus status) {
        this.healthStatus = status;
        this.lastChecked = LocalDateTime.now();
        if (status != HealthStatus.ERROR) {
            this.lastError = null;
        }
    }
    
    public void setError(String error) {
        this.lastError = error;
        this.healthStatus = HealthStatus.ERROR;
        this.lastChecked = LocalDateTime.now();
    }
}