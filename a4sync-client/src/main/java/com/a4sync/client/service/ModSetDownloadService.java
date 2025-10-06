package com.a4sync.client.service;

import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import com.a4sync.client.config.ClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service for downloading complete mod set catalogues with progress tracking
 */
@Slf4j
@RequiredArgsConstructor
public class ModSetDownloadService {
    
    private final ModManager modManager;
    private final ClientConfig config;
    
    public static class ModSetDownloadProgress {
        private final int totalMods;
        private final AtomicInteger completedMods = new AtomicInteger(0);
        private final AtomicInteger failedMods = new AtomicInteger(0);
        private volatile String currentModName = "";
        private volatile double currentModProgress = 0.0;
        private final long startTime = System.currentTimeMillis();
        private volatile boolean cancelled = false;
        
        public ModSetDownloadProgress(int totalMods) {
            this.totalMods = totalMods;
        }
        
        public int getTotalMods() { return totalMods; }
        public int getCompletedMods() { return completedMods.get(); }
        public int getFailedMods() { return failedMods.get(); }
        public String getCurrentModName() { return currentModName; }
        public double getCurrentModProgress() { return currentModProgress; }
        public boolean isCancelled() { return cancelled; }
        public void cancel() { this.cancelled = true; }
        
        public double getOverallProgress() {
            if (totalMods == 0) return 1.0;
            return (double) (completedMods.get() + failedMods.get()) / totalMods;
        }
        
        public long getEstimatedTimeRemaining() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed == 0 || completedMods.get() == 0) return -1;
            
            long avgTimePerMod = elapsed / completedMods.get();
            int remainingMods = totalMods - completedMods.get() - failedMods.get();
            return avgTimePerMod * remainingMods;
        }
        
        void setCurrentMod(String modName) { this.currentModName = modName; }
        void setCurrentModProgress(double progress) { this.currentModProgress = progress; }
        void incrementCompleted() { completedMods.incrementAndGet(); }
        void incrementFailed() { failedMods.incrementAndGet(); }
    }
    
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
    
    /**
     * Result of a mod set download operation
     */
    public static class DownloadResult {
        private final int successful;
        private final int failed;
        private final int skipped;
        
        public DownloadResult(int successful, int failed, int skipped) {
            this.successful = successful;
            this.failed = failed;
            this.skipped = skipped;
        }
        
        public int getSuccessful() { return successful; }
        public int getFailed() { return failed; }
        public int getSkipped() { return skipped; }
        public int getTotal() { return successful + failed + skipped; }
        public boolean isAllSuccessful() { return failed == 0; }
        
        @Override
        public String toString() {
            return String.format("DownloadResult{successful=%d, failed=%d, skipped=%d}", 
                    successful, failed, skipped);
        }
    }
}