package com.a4sync.client.service;

import com.a4sync.common.model.Mod;
import com.a4sync.client.config.ClientConfig;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;

public class ModManager {
    private final ClientConfig config;
    
    public ModManager(ClientConfig config) {
        this.config = config;
    }
    
    public boolean isModInstalled(Mod mod) {
        for (Path directory : config.getModDirectories()) {
            Path modPath = directory.resolve(mod.getName());
            if (Files.exists(modPath) && verifyModHash(modPath, mod.getHash())) {
                return true;
            }
        }
        return false;
    }
    
    public CompletableFuture<Void> downloadMod(Mod mod, String modSetName, String repositoryUrl) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path targetDir = selectTargetDirectory(mod);
                Path targetPath = targetDir.resolve(mod.getName());
                
                // Download the mod
                URI uri = URI.create(repositoryUrl + "/api/v1/modsets/" + modSetName + "/mods/" + mod.getName());
                Files.copy(uri.toURL().openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Verify the download
                if (!verifyModHash(targetPath, mod.getHash())) {
                    Files.delete(targetPath);
                    throw new IOException("Downloaded mod failed hash verification");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to download mod: " + mod.getName(), e);
            }
        });
    }
    
    private Path selectTargetDirectory(Mod mod) {
        // Select the first available directory with enough space
        for (Path directory : config.getModDirectories()) {
            try {
                FileStore store = Files.getFileStore(directory);
                if (store.getUsableSpace() > mod.getSize()) {
                    return directory;
                }
            } catch (IOException e) {
                // Skip this directory if we can't check space
                continue;
            }
        }
        throw new RuntimeException("No suitable directory found with enough space");
    }
    
    private boolean verifyModHash(Path modPath, String expectedHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(modPath));
            String actualHash = bytesToHex(hash);
            return actualHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
