package com.a4sync.tools;

import com.a4sync.tools.config.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.nio.file.*;
import java.util.concurrent.Callable;

@Command(
    name = "repo",
    description = "Manage repository configuration",
    subcommands = {
        RepoCommand.Init.class,
        RepoCommand.Validate.class,
        RepoCommand.Status.class
    }
)
public class RepoCommand {
    
    @Command(
        name = "init",
        description = "Initialize a new repository"
    )
    static class Init implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to repository root (optional if configured in ~/.a4sync/config.properties)", arity = "0..1")
        private Path repoPath;
        
        @Option(names = "--auth", description = "Enable authentication")
        private boolean auth = false;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager config = new ConfigManager();
            Path resolvedPath = config.resolveRepositoryPath(repoPath);
            
            if (resolvedPath == null) {
                System.err.println("Error: No repository path specified and no default configured.");
                System.err.println("Either provide a path or configure repository.default in ~/.a4sync/config.properties");
                return 1;
            }
            
            // Create directory structure
            Files.createDirectories(resolvedPath);
            Files.createDirectories(resolvedPath.resolve("modsets"));
            
            // Create example configuration
            Path configFile = resolvedPath.resolve("repository.properties");
            if (!Files.exists(configFile)) {
                Files.writeString(configFile, String.format("""
                    # A4Sync Repository Configuration
                    repository.name=My Repository
                    repository.description=My Arma 4 Mod Repository
                    repository.auth.enabled=%b
                    repository.max-chunk-size=52428800
                    repository.parallel-downloads=3
                    """, auth));
            }
            
            System.out.println("Initialized repository at: " + resolvedPath);
            return 0;
        }
    }
    
    @Command(
        name = "validate",
        description = "Validate repository structure and files"
    )
    static class Validate implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to repository root (optional if configured)", arity = "0..1")
        private Path repoPath;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager config = new ConfigManager();
            Path resolvedPath = config.resolveRepositoryPath(repoPath);
            
            if (resolvedPath == null) {
                System.err.println("Error: No repository path specified and no default configured.");
                System.err.println("Either provide a path or configure repository.default in ~/.a4sync/config.properties");
                return 1;
            }
            
            boolean valid = true;
            
            // Check directory structure
            if (!Files.exists(resolvedPath.resolve("modsets"))) {
                System.err.println("❌ Missing modsets directory");
                valid = false;
            }
            
            // Check mods
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolvedPath, "@*")) {
                for (Path mod : stream) {
                    if (Files.isDirectory(mod)) {
                        if (!Files.exists(mod.resolve("mod.json"))) {
                            System.err.println("❌ Missing mod.json in " + mod.getFileName());
                            valid = false;
                        }
                    }
                }
            }
            
            if (valid) {
                System.out.println("✅ Repository structure is valid");
                return 0;
            } else {
                return 1;
            }
        }
    }
    
    @Command(
        name = "status",
        description = "Show repository status"
    )
    static class Status implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to repository root (optional if configured)", arity = "0..1")
        private Path repoPath;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager config = new ConfigManager();
            Path resolvedPath = config.resolveRepositoryPath(repoPath);
            
            if (resolvedPath == null) {
                System.err.println("Error: No repository path specified and no default configured.");
                System.err.println("Either provide a path or configure repository.default in ~/.a4sync/config.properties");
                return 1;
            }
            
            System.out.println("Repository Status");
            System.out.println("================");
            
            // Count mods
            int modCount = 0;
            long totalSize = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolvedPath, "@*")) {
                for (Path mod : stream) {
                    if (Files.isDirectory(mod)) {
                        modCount++;
                        totalSize += Files.walk(mod)
                            .filter(Files::isRegularFile)
                            .mapToLong(p -> {
                                try {
                                    return Files.size(p);
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .sum();
                    }
                }
            }
            
            // Count mod sets
            int modSetCount = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolvedPath.resolve("modsets"), "*.json")) {
                for (Path p : stream) {
                    modSetCount++;
                }
            }
            
            System.out.printf("Mods: %d%n", modCount);
            System.out.printf("Mod Sets: %d%n", modSetCount);
            System.out.printf("Total Size: %.2f GB%n", totalSize / (1024.0 * 1024.0 * 1024.0));
            
            return 0;
        }
    }
}