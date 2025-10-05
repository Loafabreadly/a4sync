package com.a4sync.client.model;

public enum HealthStatus {
    HEALTHY("Healthy", "Repository is functioning normally"),
    DEGRADED("Degraded", "Repository has performance issues"),
    ERROR("Error", "Repository is experiencing errors"),
    UNKNOWN("Unknown", "Repository status is unknown");
    
    private final String displayName;
    private final String description;
    
    HealthStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}