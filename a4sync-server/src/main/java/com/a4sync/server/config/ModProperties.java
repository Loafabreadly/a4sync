package com.a4sync.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.nio.file.Path;

@ConfigurationProperties(prefix = "a4sync")
public class ModProperties {
    private String rootDirectory = "/a4sync";
    private boolean authenticationEnabled = false;
    private String repositoryPassword;

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
    }
}
