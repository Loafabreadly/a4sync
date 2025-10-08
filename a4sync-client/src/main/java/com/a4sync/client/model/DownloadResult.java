package com.a4sync.client.model;

/**
 * Result of a mod set download operation
 */
public class DownloadResult {
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