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
        A4SyncTools.ConfigCommand.class,
        A4SyncTools.CompletionCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class A4SyncTools implements Runnable {
    
    @Override
    public void run() {
        // Show help by default
        CommandLine.usage(this, System.out);
    }
    
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new A4SyncTools());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
    
    @Command(
        name = "completion",
        description = "Generate shell completion scripts",
        subcommands = {
            A4SyncTools.CompletionCommand.Bash.class,
            A4SyncTools.CompletionCommand.Zsh.class
        }
    )
    static class CompletionCommand implements Callable<Integer> {
        
        @Command(
            name = "bash",
            description = "Generate bash completion script"
        )
        static class Bash implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                String script = picocli.AutoComplete.bash("a4sync", new CommandLine(new A4SyncTools()));
                System.out.println(script);
                return 0;
            }
        }
        
        @Command(
            name = "zsh", 
            description = "Generate zsh completion script"
        )
        static class Zsh implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                // For zsh, we can use the bash script with some modifications
                String script = picocli.AutoComplete.bash("a4sync", new CommandLine(new A4SyncTools()));
                System.out.println("# Zsh completion script for a4sync");
                System.out.println("# Source this file or add it to your .zshrc");
                System.out.println("autoload -U compinit && compinit");
                System.out.println(script);
                return 0;
            }
        }

        @Override
        public Integer call() throws Exception {
            System.out.println("Available completion scripts:");
            System.out.println("  a4sync completion bash    Generate bash completion script");
            System.out.println("  a4sync completion zsh     Generate zsh completion script");
            System.out.println("");
            System.out.println("Usage:");
            System.out.println("  # For bash:");
            System.out.println("  a4sync completion bash > /etc/bash_completion.d/a4sync");
            System.out.println("  # Or add to ~/.bashrc:");
            System.out.println("  eval \"$(a4sync completion bash)\"");
            System.out.println("");
            System.out.println("  # For zsh:");
            System.out.println("  a4sync completion zsh > ~/.zsh/completions/_a4sync");
            System.out.println("  # Or add to ~/.zshrc:");
            System.out.println("  eval \"$(a4sync completion zsh)\"");
            return 0;
        }
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