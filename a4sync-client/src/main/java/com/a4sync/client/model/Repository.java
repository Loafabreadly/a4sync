package com.a4sync.client.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

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
    
    public Repository(String name, String url) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.url = url;
        this.useAuthentication = false;
    }
    
    public String getDisplayName() {
        return name != null ? name : url;
    }
}