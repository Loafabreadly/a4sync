package com.a4sync.tools;

import com.a4sync.common.model.ModSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

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
        @Parameters(index = "0", description = "Path to mods directory")
        private Path modsDir;
        
        @Parameters(index = "1", description = "Name of the mod set")
        private String name;
        
        @Option(names = {"-d", "--description"}, description = "Mod set description")
        private String description;
        
        @Option(names = {"-p", "--profile"}, description = "Game profile name")
        private String profile;
        
        @Option(names = {"--no-splash"}, description = "Disable splash screen")
        private boolean noSplash = false;
        
        @Override
        public Integer call() throws Exception {
            ModSet modSet = new ModSet();
            modSet.setName(name);
            modSet.setDescription(description != null ? description : name);
            
            ModSet.GameOptions options = new ModSet.GameOptions();
            options.setProfileName(profile != null ? profile : name.toLowerCase());
            options.setNoSplash(noSplash);
            modSet.setGameOptions(options);
            
            Path modSetsDir = modsDir.resolve("modsets");
            Files.createDirectories(modSetsDir);
            
            new ObjectMapper().writeValue(
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
        @Parameters(index = "0", description = "Path to mods directory")
        private Path modsDir;
        
        @Override
        public Integer call() throws Exception {
            Path modSetsDir = modsDir.resolve("modsets");
            if (!Files.exists(modSetsDir)) {
                System.out.println("No mod sets found");
                return 0;
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modSetsDir, "*.json")) {
                for (Path modSetFile : stream) {
                    ModSet modSet = new ObjectMapper().readValue(modSetFile.toFile(), ModSet.class);
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
        @Parameters(index = "0", description = "Path to mods directory")
        private Path modsDir;
        
        @Parameters(index = "1", description = "Name of the mod set")
        private String name;
        
        @Parameters(index = "2..*", description = "Mods to add")
        private List<String> mods;
        
        @Override
        public Integer call() throws Exception {
            Path modSetFile = modsDir.resolve("modsets").resolve(name + ".json");
            if (!Files.exists(modSetFile)) {
                System.err.println("Mod set not found: " + name);
                return 1;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            ModSet modSet = mapper.readValue(modSetFile.toFile(), ModSet.class);
            
            Set<String> currentMods = new HashSet<>(
                modSet.getMods() != null ? modSet.getMods() : Collections.emptyList()
            );
            currentMods.addAll(mods);
            modSet.setMods(new ArrayList<>(currentMods));
            
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
        @Parameters(index = "0", description = "Path to mods directory")
        private Path modsDir;
        
        @Parameters(index = "1", description = "Name of the mod set")
        private String name;
        
        @Parameters(index = "2..*", description = "Mods to remove")
        private List<String> mods;
        
        @Override
        public Integer call() throws Exception {
            Path modSetFile = modsDir.resolve("modsets").resolve(name + ".json");
            if (!Files.exists(modSetFile)) {
                System.err.println("Mod set not found: " + name);
                return 1;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            ModSet modSet = mapper.readValue(modSetFile.toFile(), ModSet.class);
            
            if (modSet.getMods() != null) {
                modSet.getMods().removeAll(mods);
                mapper.writeValue(modSetFile.toFile(), modSet);
                System.out.println("Removed mods from " + name);
            }
            return 0;
        }
    }
}