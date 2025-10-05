package com.a4sync.client.config;

import lombok.Data;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.a4sync.common.model.GameOptions;

@Data
public class ClientConfig {
    private String serverUrl;
    private String repositoryPassword;
    private boolean useAuthentication;
    private String localModsPath;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private List<Path> modDirectories = new ArrayList<>();
    private Path steamPath;
    private Path gamePath;
    private GameOptions defaultGameOptions = new GameOptions();

    public void addModDirectory(Path path) {
        if (!modDirectories.contains(path)) {
            modDirectories.add(path);
        }
    }

    public List<Path> getModDirectories() {
        return Collections.unmodifiableList(modDirectories);
    }
}
