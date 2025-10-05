package com.a4sync.client.service;

import com.a4sync.common.model.Mod;
import com.a4sync.client.config.ClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class ModManager {
    private final ClientConfig config;
    private final ChunkedDownloadService downloadService;
    
    public ModManager(ClientConfig config) {
        this.config = config;
        this.downloadService = new ChunkedDownloadService();
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
        return downloadMod(mod, modSetName, repositoryUrl, null);
    }
    
    public CompletableFuture<Void> downloadMod(Mod mod, String modSetName, String repositoryUrl, 
            Consumer<ChunkedDownloadService.DownloadProgress> progressCallback) {
        Path targetDir = selectTargetDirectory(mod);
        Path targetPath = targetDir.resolve(mod.getName());
        
        String downloadUrl = repositoryUrl + "/api/v1/modsets/" + modSetName + "/mods/" + mod.getName();
        log.info("Downloading mod {} to {}", mod.getName(), targetPath);
        
        return downloadService.downloadFile(downloadUrl, targetPath, mod.getHash(), progressCallback)
            .thenCompose(success -> {
                if (!success) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Failed to download mod: " + mod.getName()));
                }
                
                // Additional verification using our existing method
                if (mod.getHash() != null && !verifyModHash(targetPath, mod.getHash())) {
                    try {
                        Files.deleteIfExists(targetPath);
                    } catch (IOException e) {
                        log.warn("Failed to delete invalid mod file: {}", targetPath, e);
                    }
                    return CompletableFuture.failedFuture(
                        new IOException("Downloaded mod failed hash verification: " + mod.getName()));
                }
                
                log.info("Successfully downloaded and verified mod: {}", mod.getName());
                return CompletableFuture.completedFuture(null);
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
