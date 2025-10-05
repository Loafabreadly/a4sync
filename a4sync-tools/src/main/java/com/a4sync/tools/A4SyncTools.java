package com.a4sync.tools;

import com.a4sync.tools.config.ConfigManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(
    name = "a4sync",
    version = "1.0.0",
    description = "A4Sync mod repository management tools",
    subcommands = {
        ModCommand.class,
        ModSetCommand.class,
        RepoCommand.class,
        A4SyncTools.ConfigCommand.class
    }
)
public class A4SyncTools implements Runnable {
    
    @Override
    public void run() {
        // Show help by default
        CommandLine.usage(this, System.out);
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new A4SyncTools()).execute(args);
        System.exit(exitCode);
    }
    
    @Command(
        name = "config",
        description = "Manage configuration settings",
        subcommands = {
            A4SyncTools.ConfigCommand.Init.class
        }
    )
    static class ConfigCommand implements Callable<Integer> {
        
        @Command(
            name = "init",
            description = "Create example configuration file"
        )
        static class Init implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                try {
                    ConfigManager config = new ConfigManager();
                    config.createExampleConfig();
                    return 0;
                } catch (IOException e) {
                    System.err.println("Error creating configuration file: " + e.getMessage());
                    return 1;
                }
            }
        }
        
        @Override
        public Integer call() throws Exception {
            // Show help by default
            CommandLine.usage(this, System.out);
            return 0;
        }
    }
}