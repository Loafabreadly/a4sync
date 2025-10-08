package com.a4sync.client.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Progress tracking for mod set downloads
 */
public class ModSetDownloadProgress {
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
    
    public void setCurrentMod(String modName) { this.currentModName = modName; }
    public void setCurrentModProgress(double progress) { this.currentModProgress = progress; }
    public void incrementCompleted() { completedMods.incrementAndGet(); }
    public void incrementFailed() { failedMods.incrementAndGet(); }
}