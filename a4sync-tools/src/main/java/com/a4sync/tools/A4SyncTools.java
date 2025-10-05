package com.a4sync.tools;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "a4sync",
    version = "1.0.0",
    description = "A4Sync mod repository management tools",
    subcommands = {
        ModCommand.class,
        ModSetCommand.class,
        RepoCommand.class
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
}