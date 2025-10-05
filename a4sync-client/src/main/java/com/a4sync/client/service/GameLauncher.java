package com.a4sync.client.service;

import com.a4sync.common.model.GameOptions;
import com.a4sync.common.model.ModSet;
import com.a4sync.client.config.ClientConfig;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameLauncher {
    private final ClientConfig config;
    
    public GameLauncher(ClientConfig config) {
        this.config = config;
    }
    
    public void launchGame(ModSet modSet) throws IOException {
        if (config.getSteamPath() == null || config.getGamePath() == null) {
            throw new IllegalStateException("Steam or game path not configured");
        }

        List<String> command = new ArrayList<>();
        command.add(config.getSteamPath().toString());
        command.add("-applaunch");
        command.add("107410"); // Arma 4 Steam App ID
        
        // Add mod parameters
        if (modSet != null && !modSet.getMods().isEmpty()) {
            command.add("-mod=" + buildModParameter(modSet));
        }
        
        // Add game options  
        GameOptions options = modSet != null ? modSet.getGameOptions() : config.getDefaultGameOptionsObject();
        if (options != null) {
            if (options.isNoSplash()) {
                command.add("-nosplash");
            }
            if (options.getProfileName() != null && !options.getProfileName().isEmpty()) {
                command.add("-name=" + options.getProfileName());
            }
            if (options.getAdditionalParameters() != null && !options.getAdditionalParameters().isEmpty()) {
                command.addAll(List.of(options.getAdditionalParameters().split(" ")));
            }
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(config.getGamePath().toFile());
        processBuilder.start();
    }
    
    private String buildModParameter(ModSet modSet) {
        return modSet.getMods().stream()
                .map(mod -> {
                    for (Path directory : config.getModDirectories()) {
                        Path modPath = directory.resolve(mod.getName());
                        if (modPath.toFile().exists()) {
                            return modPath.toString();
                        }
                    }
                    return null;
                })
                .filter(path -> path != null)
                .collect(Collectors.joining(";"));
    }
}
