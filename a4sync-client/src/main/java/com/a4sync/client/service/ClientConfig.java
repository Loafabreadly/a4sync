package com.a4sync.client.service;

import com.a4sync.common.model.GameOptions;
import com.a4sync.common.model.GameType;
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
    private static final String PREF_DEFAULT_GAME_TYPE = "defaultGameType";
    private static final String PREF_DEFAULT_PROFILE = "defaultProfile";
    private static final String PREF_NO_SPLASH = "noSplash";
    private static final String PREF_ADDITIONAL_PARAMS = "additionalParams";
    private static final String PREF_USE_AUTH = "useAuthentication";
    private static final String PREF_REPO_PASSWORD = "repositoryPassword";
    private static final String PREF_MAX_RETRIES = "maxRetries";
    private static final String PREF_RETRY_DELAY = "retryDelayMs";
    
    private List<Path> modDirectories;
    private Path gamePath;
    private Path steamPath;
    private GameOptions defaultGameOptions;
    
    // Authentication settings
    private boolean useAuthentication = false;
    private String repositoryPassword = "";
    
    // Retry settings
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    
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
        
        // Load default game options
        defaultGameOptions = new GameOptions();
        String savedGameType = prefs.get(PREF_DEFAULT_GAME_TYPE, GameType.ARMA_4.name());
        try {
            defaultGameOptions.setGameType(GameType.valueOf(savedGameType));
        } catch (IllegalArgumentException e) {
            defaultGameOptions.setGameType(GameType.ARMA_4);
        }
        defaultGameOptions.setProfileName(prefs.get(PREF_DEFAULT_PROFILE, ""));
        defaultGameOptions.setNoSplash(prefs.getBoolean(PREF_NO_SPLASH, false));
        defaultGameOptions.setAdditionalParameters(prefs.get(PREF_ADDITIONAL_PARAMS, ""));
        
        // Load authentication and retry settings
        useAuthentication = prefs.getBoolean(PREF_USE_AUTH, false);
        repositoryPassword = prefs.get(PREF_REPO_PASSWORD, "");
        maxRetries = prefs.getInt(PREF_MAX_RETRIES, 3);
        retryDelayMs = prefs.getLong(PREF_RETRY_DELAY, 1000);
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
    
    public void saveDefaultGameOptions(GameOptions options) {
        this.defaultGameOptions = options;
        if (options != null) {
            prefs.put(PREF_DEFAULT_GAME_TYPE, options.getGameType().name());
            prefs.put(PREF_DEFAULT_PROFILE, options.getProfileName() != null ? options.getProfileName() : "");
            prefs.putBoolean(PREF_NO_SPLASH, options.isNoSplash());
            prefs.put(PREF_ADDITIONAL_PARAMS, options.getAdditionalParameters() != null ? options.getAdditionalParameters() : "");
        }
    }
    
    public void setUseAuthentication(boolean useAuth) {
        this.useAuthentication = useAuth;
        prefs.putBoolean(PREF_USE_AUTH, useAuth);
    }
    
    public void setRepositoryPassword(String password) {
        this.repositoryPassword = password;
        prefs.put(PREF_REPO_PASSWORD, password);
    }
    
    public void setMaxRetries(int retries) {
        this.maxRetries = retries;
        prefs.putInt(PREF_MAX_RETRIES, retries);
    }
    
    public void setRetryDelayMs(long delay) {
        this.retryDelayMs = delay;
        prefs.putLong(PREF_RETRY_DELAY, delay);
    }
}
