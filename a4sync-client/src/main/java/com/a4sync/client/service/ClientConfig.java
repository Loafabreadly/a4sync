package com.a4sync.client.service;

import com.a4sync.common.model.GameOptions;
import lombok.Data;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

@Data
public class ClientConfig {
    private static final String PREF_MOD_DIRECTORIES = "modDirectories";
    private static final String PREF_GAME_PATH = "gamePath";
    private static final String PREF_STEAM_PATH = "steamPath";
    
    private List<Path> modDirectories;
    private Path gamePath;
    private Path steamPath;
    private GameOptions defaultGameOptions;
    
    private static final Preferences prefs = Preferences.userNodeForPackage(ClientConfig.class);
    
    public ClientConfig() {
        loadConfig();
    }
    
    public void addModDirectory(Path directory) {
        if (modDirectories == null) {
            modDirectories = new ArrayList<>();
        }
        if (!modDirectories.contains(directory)) {
            modDirectories.add(directory);
            saveModDirectories();
        }
    }
    
    public void removeModDirectory(Path directory) {
        if (modDirectories != null) {
            modDirectories.remove(directory);
            saveModDirectories();
        }
    }
    
    private void loadConfig() {
        modDirectories = new ArrayList<>();
        String savedDirs = prefs.get(PREF_MOD_DIRECTORIES, "");
        if (!savedDirs.isEmpty()) {
            for (String dir : savedDirs.split(";")) {
                modDirectories.add(Path.of(dir));
            }
        }
        
        String savedGamePath = prefs.get(PREF_GAME_PATH, "");
        if (!savedGamePath.isEmpty()) {
            gamePath = Path.of(savedGamePath);
        }
        
        String savedSteamPath = prefs.get(PREF_STEAM_PATH, "");
        if (!savedSteamPath.isEmpty()) {
            steamPath = Path.of(savedSteamPath);
        }
    }
    
    private void saveModDirectories() {
        if (modDirectories != null) {
            String dirs = String.join(";", modDirectories.stream()
                    .map(Path::toString)
                    .toList());
            prefs.put(PREF_MOD_DIRECTORIES, dirs);
        }
    }
    
    public void saveGamePath(Path path) {
        this.gamePath = path;
        prefs.put(PREF_GAME_PATH, path.toString());
    }
    
    public void saveSteamPath(Path path) {
        this.steamPath = path;
        prefs.put(PREF_STEAM_PATH, path.toString());
    }
}
