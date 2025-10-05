package com.a4sync.tools.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages configuration settings for A4Sync tools.
 * Reads from ~/.a4sync/config.properties file as documented in CLI reference.
 */
public class ConfigManager {
    private static final String CONFIG_DIR = ".a4sync";
    private static final String CONFIG_FILE = "config.properties";
    private static final String REPOSITORY_DEFAULT_KEY = "repository.default";
    
    private final Properties properties;
    private final Path configPath;
    
    public ConfigManager() {
        this.configPath = getUserHome().resolve(CONFIG_DIR).resolve(CONFIG_FILE);
        this.properties = new Properties();
        loadConfig();
    }
    
    private void loadConfig() {
        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                properties.load(input);
            } catch (IOException e) {
                System.err.println("Warning: Could not load config file: " + configPath);
                System.err.println("Using default settings. Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets the default repository path from config.properties.
     * @return The default repository path, or null if not configured
     */
    public Path getDefaultRepositoryPath() {
        String defaultPath = properties.getProperty(REPOSITORY_DEFAULT_KEY);
        if (defaultPath != null && !defaultPath.trim().isEmpty()) {
            return Paths.get(defaultPath.trim());
        }
        return null;
    }
    
    /**
     * Gets a repository path, using the provided path if specified,
     * otherwise falling back to the configured default.
     * @param providedPath The path provided by the user (may be null)
     * @return The path to use, or null if neither provided nor configured
     */
    public Path resolveRepositoryPath(Path providedPath) {
        if (providedPath != null) {
            return providedPath;
        }
        return getDefaultRepositoryPath();
    }
    
    /**
     * Gets the network timeout setting.
     * @return Timeout in seconds, defaults to 30
     */
    public int getNetworkTimeout() {
        String timeout = properties.getProperty("network.timeout", "30");
        try {
            return Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            return 30;
        }
    }
    
    /**
     * Gets the network retry count.
     * @return Number of retries, defaults to 3
     */
    public int getNetworkRetries() {
        String retries = properties.getProperty("network.retries", "3");
        try {
            return Integer.parseInt(retries);
        } catch (NumberFormatException e) {
            return 3;
        }
    }
    
    /**
     * Gets the logging level setting.
     * @return Logging level, defaults to "INFO"
     */
    public String getLoggingLevel() {
        return properties.getProperty("logging.level", "INFO");
    }
    
    /**
     * Gets the log file path setting.
     * @return Log file path, defaults to ~/.a4sync/a4sync.log
     */
    public Path getLogFile() {
        String logFile = properties.getProperty("logging.file", "~/.a4sync/a4sync.log");
        if (logFile.startsWith("~/")) {
            return getUserHome().resolve(logFile.substring(2));
        }
        return Paths.get(logFile);
    }
    
    private Path getUserHome() {
        return Paths.get(System.getProperty("user.home"));
    }
    
    /**
     * Creates an example configuration file for the user.
     * @throws IOException if the file cannot be created
     */
    public void createExampleConfig() throws IOException {
        Path configDir = configPath.getParent();
        Files.createDirectories(configDir);
        
        String exampleConfig = """
                # A4Sync Tools Configuration
                
                # Default repository path (used when no path is specified)
                repository.default=/path/to/repository
                
                # Network settings
                network.timeout=30
                network.retries=3
                
                # Logging
                logging.level=INFO
                logging.file=~/.a4sync/a4sync.log
                """;
        
        Files.writeString(configPath, exampleConfig);
        System.out.println("Created example configuration at: " + configPath);
        System.out.println("Please edit this file to set your default repository path.");
    }
}