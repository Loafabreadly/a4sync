package com.a4sync.client.model;

import lombok.Data;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks progress of chunked downloads with real-time statistics
 */
@Data
public class DownloadProgress {
    private final long totalBytes;
    private final AtomicLong downloadedBytes = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    private volatile boolean cancelled = false;
    private volatile String status = "Starting";
    
    public DownloadProgress(long totalBytes) {
        this.totalBytes = totalBytes;
    }
    
    public double getProgressPercentage() {
        if (totalBytes <= 0) return 0;
        return (double) downloadedBytes.get() / totalBytes * 100.0;
    }
    
    public long getDownloadSpeed() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed <= 0) return 0;
        return (downloadedBytes.get() * 1000) / elapsed; // bytes per second
    }
    
    public long getEstimatedTimeRemaining() {
        long speed = getDownloadSpeed();
        if (speed <= 0) return -1;
        return (totalBytes - downloadedBytes.get()) / speed;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public AtomicLong getDownloadedBytes() {
        return downloadedBytes;
    }
}