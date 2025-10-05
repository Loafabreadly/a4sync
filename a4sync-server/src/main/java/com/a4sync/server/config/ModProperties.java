package com.a4sync.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.Path;

@ConfigurationProperties(prefix = "a4sync")
public class ModProperties {
    private String rootDirectory = "/a4sync";
    private boolean authenticationEnabled = false;
    private String repositoryPassword;
    private String repositoryPasswordHash; // BCrypt hash of the password
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getModSetsDirectory() {
        return rootDirectory;
    }

    public String getModsDirectory(String modSetName) {
        return Path.of(rootDirectory, modSetName).toString();
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public void setAuthenticationEnabled(boolean authenticationEnabled) {
        this.authenticationEnabled = authenticationEnabled;
    }

    public String getRepositoryPassword() {
        return repositoryPassword;
    }

    public void setRepositoryPassword(String repositoryPassword) {
        this.repositoryPassword = repositoryPassword;
        // Generate BCrypt hash when password is set
        if (repositoryPassword != null) {
            this.repositoryPasswordHash = passwordEncoder.encode(repositoryPassword);
        }
    }
    
    public String getRepositoryPasswordHash() {
        return repositoryPasswordHash;
    }
    
    public void setRepositoryPasswordHash(String repositoryPasswordHash) {
        this.repositoryPasswordHash = repositoryPasswordHash;
    }
    
    /**
     * Verify a plain text password against the stored BCrypt hash
     */
    public boolean verifyPassword(String plainTextPassword) {
        if (repositoryPasswordHash == null || plainTextPassword == null) {
            return false;
        }
        return passwordEncoder.matches(plainTextPassword, repositoryPasswordHash);
    }
}
