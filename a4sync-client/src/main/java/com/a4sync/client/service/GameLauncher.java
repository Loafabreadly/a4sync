package com.a4sync.client.service;

import com.a4sync.common.model.GameOptions;
import com.a4sync.common.model.GameType;
import com.a4sync.common.model.ModSet;
import com.a4sync.client.config.ClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GameLauncher {
    private final ClientConfig config;
    
    public GameLauncher(ClientConfig config) {
        this.config = config;
    }
    
    public void launchGame(ModSet modSet) throws IOException {
        if (config.getSteamPath() == null) {
            throw new IllegalStateException("Steam path not configured");
        }

        // Use client-side game options instead of server-side modset options
        GameOptions options = config.getDefaultGameOptionsObject();
        GameType gameType = options != null && options.getGameType() != null ? 
                           options.getGameType() : GameType.ARMA_4;

        List<String> command = buildSteamCommand(gameType, modSet, options);
        
        log.info("Launching {} via Steam with command: {}", gameType.getDisplayName(), command);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (config.getGamePath() != null) {
            processBuilder.directory(config.getGamePath().toFile());
        }
        processBuilder.start();
    }
    
    public void launchGameWithType(ModSet modSet, GameType gameType) throws IOException {
        if (config.getSteamPath() == null) {
            throw new IllegalStateException("Steam path not configured");
        }

        // Use client-side game options instead of server-side modset options
        GameOptions options = config.getDefaultGameOptionsObject();
        
        List<String> command = buildSteamCommand(gameType, modSet, options);
        
        log.info("Launching {} via Steam with command: {}", gameType.getDisplayName(), command);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (config.getGamePath() != null) {
            processBuilder.directory(config.getGamePath().toFile());
        }
        processBuilder.start();
    }
    
    private List<String> buildSteamCommand(GameType gameType, ModSet modSet, GameOptions options) {
        List<String> command = new ArrayList<>();
        command.add(config.getSteamPath().toString());
        command.add("-applaunch");
        command.add(gameType.getSteamAppId());
        
        // Add mod parameters
        if (modSet != null && modSet.getMods() != null && !modSet.getMods().isEmpty()) {
            String modParams = buildModParameter(modSet);
            if (!modParams.isEmpty()) {
                command.add("-mod=" + modParams);
            }
        }
        
        // Add game options  
        if (options != null) {
            if (options.isNoSplash()) {
                command.add("-nosplash");
            }
            if (options.getProfileName() != null && !options.getProfileName().isEmpty()) {
                command.add("-name=" + options.getProfileName());
            }
            if (options.getAdditionalParameters() != null && !options.getAdditionalParameters().isEmpty()) {
                // Split additional parameters properly, handling quoted arguments
                String[] params = options.getAdditionalParameters().split("\\s+");
                for (String param : params) {
                    if (!param.trim().isEmpty()) {
                        command.add(param.trim());
                    }
                }
            }
        }
        
        return command;
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
