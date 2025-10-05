package com.a4sync.tools;

import com.a4sync.common.model.ModIndex;
import com.a4sync.tools.config.ConfigManager;
import com.a4sync.tools.util.ModUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.concurrent.Callable;

@Command(
    name = "mod",
    description = "Manage individual mods",
    subcommands = {
        ModCommand.Create.class,
        ModCommand.Update.class,
        ModCommand.List.class
    }
)
public class ModCommand {
    
    @Command(
        name = "create",
        description = "Create a new mod configuration"
    )
    static class Create implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to mod directory")
        private Path modPath;
        
        @Option(names = {"-v", "--version"}, description = "Mod version")
        private String version = "1.0.0";
        
        @Override
        public Integer call() throws Exception {
            ModUtils.createModIndex(modPath, version);
            System.out.println("Created mod configuration for: " + modPath);
            return 0;
        }
    }
    
    @Command(
        name = "update",
        description = "Update an existing mod configuration"
    )
    static class Update implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to mod directory")
        private Path modPath;
        
        @Override
        public Integer call() throws Exception {
            ModUtils.updateModIndex(modPath);
            System.out.println("Updated mod configuration for: " + modPath);
            return 0;
        }
    }
    
    @Command(
        name = "list",
        description = "List all mods in a directory"
    )
    static class List implements Callable<Integer> {
        @Parameters(index = "0", description = "Path to mods directory")
        private Path modsDir;
        
        @Override
        public Integer call() throws Exception {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "@*")) {
                for (Path mod : stream) {
                    if (Files.isDirectory(mod)) {
                        Path indexPath = mod.resolve("mod.json");
                        if (Files.exists(indexPath)) {
                            ModIndex index = new ObjectMapper().readValue(indexPath.toFile(), ModIndex.class);
                            System.out.printf("%s (v%s) - %.2f GB%n", 
                                index.getName(), 
                                index.getVersion(), 
                                index.getTotalSize() / (1024.0 * 1024.0 * 1024.0));
                        } else {
                            System.out.printf("%s (not indexed)%n", mod.getFileName());
                        }
                    }
                }
            }
            return 0;
        }
    }
}