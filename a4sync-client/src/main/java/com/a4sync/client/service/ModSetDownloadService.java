package com.a4sync.client.service;

import com.a4sync.client.model.DownloadResult;
import com.a4sync.client.model.ModSetDownloadProgress;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import com.a4sync.client.config.ClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for downloading complete mod set catalogues with progress tracking
 */
@Slf4j
@RequiredArgsConstructor
public class ModSetDownloadService {
    
    private final ModManager modManager;
    private final ClientConfig config;
    

    
    /**
     * Downloads an entire mod set's catalogue of mods
     * @param modSet The mod set to download
     * @param repositoryUrl The repository URL
     * @param progressCallback Optional progress callback
     * @return CompletableFuture that completes when all downloads finish
     */
    public CompletableFuture<DownloadResult> downloadModSet(
            ModSet modSet, 
            String repositoryUrl,
            Consumer<ModSetDownloadProgress> progressCallback) {
        
        List<Mod> modsToDownload = modSet.getMods();
        if (modsToDownload == null || modsToDownload.isEmpty()) {
            return CompletableFuture.completedFuture(new DownloadResult(0, 0, 0));
        }
        
        ModSetDownloadProgress progress = new ModSetDownloadProgress(modsToDownload.size());
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting download of mod set '{}' with {} mods", modSet.getName(), modsToDownload.size());
            
            for (Mod mod : modsToDownload) {
                if (progress.isCancelled()) {
                    log.info("Download cancelled for mod set '{}'", modSet.getName());
                    break;
                }
                
                // Skip already installed mods
                if (modManager.isModInstalled(mod)) {
                    log.debug("Mod '{}' already installed, skipping", mod.getName());
                    progress.incrementCompleted();
                    if (progressCallback != null) {
                        progressCallback.accept(progress);
                    }
                    continue;
                }
                
                progress.setCurrentMod(mod.getName());
                progress.setCurrentModProgress(0.0);
                
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
                
                try {
                    // Download individual mod with progress tracking
                    modManager.downloadMod(mod, modSet.getName(), repositoryUrl, 
                        modProgress -> {
                            progress.setCurrentModProgress(modProgress.getProgressPercentage() / 100.0);
                            if (progressCallback != null) {
                                progressCallback.accept(progress);
                            }
                        }).get(); // Block until this mod completes
                    
                    progress.incrementCompleted();
                    log.info("Successfully downloaded mod: {}", mod.getName());
                    
                } catch (Exception e) {
                    progress.incrementFailed();
                    log.error("Failed to download mod '{}': {}", mod.getName(), e.getMessage(), e);
                }
                
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
            }
            
            int successful = progress.getCompletedMods();
            int failed = progress.getFailedMods();
            int skipped = modsToDownload.size() - successful - failed;
            
            log.info("Mod set '{}' download completed: {} successful, {} failed, {} skipped", 
                    modSet.getName(), successful, failed, skipped);
            
            return new DownloadResult(successful, failed, skipped);
        });
    }
    

}