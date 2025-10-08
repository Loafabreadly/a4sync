package com.a4sync.tools;

import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import com.a4sync.common.model.ModIndex;
import com.a4sync.tools.config.ConfigManager;
import com.a4sync.tools.util.ModUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

@Command(
    name = "modset",
    description = "Manage mod sets",
    subcommands = {
        ModSetCommand.Create.class,
        ModSetCommand.List.class,
        ModSetCommand.Add.class,
        ModSetCommand.Remove.class
    }
)
public class ModSetCommand {
    
    @Command(
        name = "create",
        description = "Create a new mod set"
    )
    static class Create implements Callable<Integer> {
        @Option(names = {"-r", "--repository"}, description = "Path to repository directory (optional if configured in ~/.a4sync/config.properties)")
        private Path modsDir;
        
        @Parameters(index = "0", description = "Name of the mod set")
        private String name;
        
        @Option(names = {"-d", "--description"}, description = "Mod set description")
        private String description;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager config = new ConfigManager();
            Path resolvedPath = config.resolveRepositoryPath(modsDir);
            
            if (resolvedPath == null) {
                System.err.println("Error: No repository path specified and no default configured.");
                System.err.println("Either provide a path or configure repository.default in ~/.a4sync/config.properties");
                return 1;
            }
            
            ModSet modSet = new ModSet();
            modSet.setName(name);
            modSet.setDescription(description != null ? description : name);
            modSet.setVersion("1.0.0"); // Default version
            modSet.setLastUpdated(java.time.LocalDateTime.now());
            
            Path modSetsDir = resolvedPath.resolve("modsets");
            Files.createDirectories(modSetsDir);
            
            ModUtils.getObjectMapper().writeValue(
                modSetsDir.resolve(name + ".json").toFile(),
                modSet
            );
            
            System.out.println("Created mod set: " + name);
            return 0;
        }
    }
    
    @Command(
        name = "list",
        description = "List all mod sets"
    )
    static class List implements Callable<Integer> {
        @Option(names = {"-r", "--repository"}, description = "Path to repository directory (optional if configured in ~/.a4sync/config.properties)")
        private Path modsDir;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager config = new ConfigManager();
            Path resolvedPath = config.resolveRepositoryPath(modsDir);
            
            if (resolvedPath == null) {
                System.err.println("Error: No repository path specified and no default configured.");
                System.err.println("Either provide a path or configure repository.default in ~/.a4sync/config.properties");
                return 1;
            }
            Path modSetsDir = resolvedPath.resolve("modsets");
            if (!Files.exists(modSetsDir)) {
                System.out.println("No mod sets found");
                return 0;
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modSetsDir, "*.json")) {
                for (Path modSetFile : stream) {
                    if (Files.exists(modSetFile)) {
                    ModSet modSet = ModUtils.getObjectMapper().readValue(modSetFile.toFile(), ModSet.class);
                    System.out.println("Mod Set: " + modSet.getName());
                    System.out.printf("%s - %s%n", modSet.getName(), modSet.getDescription());
                    if (modSet.getMods() != null) {
                        modSet.getMods().forEach(mod -> System.out.printf("  - %s%n", mod));
                    }
                }
            }
            return 0;
        }
    }
    
    @Command(
        name = "add",
        description = "Add mods to a mod set"
    )
    static class Add implements Callable<Integer> {
        @Option(names = {"-r", "--repository"}, description = "Path to repository directory (optional if configured in ~/.a4sync/config.properties)")
        private Path modsDir;
        
        @Parameters(index = "0", description = "Name of the mod set")
        private String name;
        
        @Parameters(index = "1..*", description = "Mods to add")
        private java.util.List<String> mods;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager config = new ConfigManager();
            Path resolvedPath = config.resolveRepositoryPath(modsDir);
            
            if (resolvedPath == null) {
                System.err.println("Error: No repository path specified and no default configured.");
                System.err.println("Either provide a path or configure repository.default in ~/.a4sync/config.properties");
                return 1;
            }
            
            Path modSetFile = resolvedPath.resolve("modsets").resolve(name + ".json");
            if (!Files.exists(modSetFile)) {
                System.err.println("Mod set not found: " + name);
                return 1;
            }
            
            ObjectMapper mapper = ModUtils.getObjectMapper();
            ModSet modSet = mapper.readValue(modSetFile.toFile(), ModSet.class);
            
            // Build a map of existing mods for fast lookup
            Map<String, Mod> existingMods = new HashMap<>();
            if (modSet.getMods() != null) {
                for (Mod mod : modSet.getMods()) {
                    existingMods.put(mod.getName(), mod);
                }
            }
            
            // Add new mods while preserving existing ones
            for (String modName : mods) {
                if (existingMods.containsKey(modName)) {
                    System.out.println("Mod " + modName + " already exists in modset");
                    continue;
                }
                
                // Verify the mod exists in the repository
                Path modPath = resolvedPath.resolve(modName);
                if (!Files.exists(modPath) || !Files.isDirectory(modPath)) {
                    System.err.println("Warning: Mod directory not found: " + modName);
                    System.err.println("Make sure to create the mod with 'a4sync mod create " + modPath + "' first");
                    continue;
                }
                
                try {
                    // Read mod metadata from mod.json
                    ModIndex modIndex = ModUtils.readModIndex(modPath);
                    
                    // Create Mod object with proper metadata
                    Mod mod = new Mod();
                    mod.setName(modIndex.getName());
                    mod.setVersion(modIndex.getVersion());
                    mod.setSize(modIndex.getTotalSize());
                    mod.setHash(modIndex.getHash());
                    // Server-relative download URL will be set by the server at runtime
                    mod.setDownloadUrl("/api/v1/modsets/" + name + "/mods/" + modIndex.getName());
                    
                    existingMods.put(modName, mod);
                    System.out.println("Added " + modName + " (v" + modIndex.getVersion() + ", " + 
                                     formatSize(modIndex.getTotalSize()) + ")");
                    
                } catch (IOException e) {
                    System.err.println("Error reading mod metadata for " + modName + ": " + e.getMessage());
                    System.err.println("Make sure to run 'a4sync mod create " + modPath + "' first");
                    continue;
                }
            }
            
            // Update the modset with all mods and recalculate total size
            java.util.List<Mod> updatedMods = new ArrayList<>(existingMods.values());
            modSet.setMods(updatedMods);
            
            // Recalculate total size
            long totalSize = updatedMods.stream().mapToLong(Mod::getSize).sum();
            modSet.setTotalSize(totalSize);
            modSet.setLastUpdated(java.time.LocalDateTime.now());
            
            mapper.writeValue(modSetFile.toFile(), modSet);
            System.out.println("Added mods to " + name);
            return 0;
        }
    }
    
    @Command(
        name = "remove",
        description = "Remove mods from a mod set"
    )
    static class Remove implements Callable<Integer> {
        @Option(names = {"-r", "--repository"}, description = "Path to repository directory (optional if configured in ~/.a4sync/config.properties)")
        private Path modsDir;
        
        @Parameters(index = "0", description = "Name of the mod set")
        private String name;
        
        @Parameters(index = "1..*", description = "Mods to remove")
        private java.util.List<String> mods;
        
        @Override
        public Integer call() throws Exception {
            ConfigManager config = new ConfigManager();
            Path resolvedPath = config.resolveRepositoryPath(modsDir);
            
            if (resolvedPath == null) {
                System.err.println("Error: No repository path specified and no default configured.");
                System.err.println("Either provide a path with -r or configure repository.default in ~/.a4sync/config.properties");
                return 1;
            }
            
            Path modSetFile = resolvedPath.resolve("modsets").resolve(name + ".json");
            if (!Files.exists(modSetFile)) {
                System.err.println("Mod set not found: " + name);
                return 1;
            }
            
            ObjectMapper mapper = ModUtils.getObjectMapper();
            ModSet modSet = mapper.readValue(modSetFile.toFile(), ModSet.class);
            
            if (modSet.getMods() != null) {
                java.util.List<Mod> updatedMods = new ArrayList<>();
                for (Mod mod : modSet.getMods()) {
                    if (!mods.contains(mod.getName())) {
                        updatedMods.add(mod);
                    } else {
                        System.out.println("Removed " + mod.getName());
                    }
                }
                
                modSet.setMods(updatedMods);
                
                // Recalculate total size
                long totalSize = updatedMods.stream().mapToLong(Mod::getSize).sum();
                modSet.setTotalSize(totalSize);
                modSet.setLastUpdated(java.time.LocalDateTime.now());
                
                mapper.writeValue(modSetFile.toFile(), modSet);
                System.out.println("Updated modset " + name + " (total size: " + formatSize(totalSize) + ")");
            }
            return 0;
        }
    }
    
    /**
     * Formats file size in human-readable format
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
}