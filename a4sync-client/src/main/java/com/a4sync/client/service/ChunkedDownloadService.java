package com.a4sync.client.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
public class ChunkedDownloadService {
    
    private static final int DEFAULT_CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 8KB buffer
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    @Data
    public static class DownloadProgress {
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
    
    public CompletableFuture<Boolean> downloadFile(
            String url, 
            Path destinationPath, 
            Consumer<DownloadProgress> progressCallback) {
        return downloadFile(url, destinationPath, null, progressCallback);
    }
    
    public CompletableFuture<Boolean> downloadFile(
            String url, 
            Path destinationPath, 
            String expectedChecksum,
            Consumer<DownloadProgress> progressCallback) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create parent directories if they don't exist
                Files.createDirectories(destinationPath.getParent());
                
                // Check if file already exists and is complete
                if (Files.exists(destinationPath) && expectedChecksum != null) {
                    String existingChecksum = calculateFileChecksum(destinationPath);
                    if (expectedChecksum.equals(existingChecksum)) {
                        log.info("File already exists and checksum matches: {}", destinationPath);
                        return true;
                    }
                }
                
                // Get file size from server
                long fileSize = getFileSize(url);
                if (fileSize <= 0) {
                    log.warn("Could not determine file size for: {}", url);
                    return downloadSequential(url, destinationPath, expectedChecksum, progressCallback);
                }
                
                DownloadProgress progress = new DownloadProgress(fileSize);
                
                // Check if partial file exists (resume capability)
                long existingSize = 0;
                if (Files.exists(destinationPath)) {
                    existingSize = Files.size(destinationPath);
                    if (existingSize >= fileSize) {
                        // File is complete or larger than expected, verify checksum
                        if (expectedChecksum != null) {
                            String actualChecksum = calculateFileChecksum(destinationPath);
                            if (expectedChecksum.equals(actualChecksum)) {
                                progress.getDownloadedBytes().set(fileSize);
                                progress.setStatus("Complete");
                                if (progressCallback != null) {
                                    progressCallback.accept(progress);
                                }
                                return true;
                            } else {
                                log.warn("Existing file checksum mismatch, re-downloading: {}", destinationPath);
                                Files.delete(destinationPath);
                                existingSize = 0;
                            }
                        }
                    } else {
                        progress.getDownloadedBytes().set(existingSize);
                        log.info("Resuming download from byte {}", existingSize);
                    }
                }
                
                // Download remaining content
                boolean success = downloadWithResume(url, destinationPath, existingSize, progress, progressCallback);
                
                if (success && expectedChecksum != null) {
                    progress.setStatus("Verifying checksum");
                    if (progressCallback != null) {
                        progressCallback.accept(progress);
                    }
                    
                    String actualChecksum = calculateFileChecksum(destinationPath);
                    if (!expectedChecksum.equals(actualChecksum)) {
                        log.error("Checksum verification failed for {}: expected {}, got {}", 
                                destinationPath, expectedChecksum, actualChecksum);
                        Files.deleteIfExists(destinationPath);
                        return false;
                    }
                }
                
                progress.setStatus("Complete");
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
                
                return success;
                
            } catch (Exception e) {
                log.error("Download failed for {}: {}", url, e.getMessage(), e);
                return false;
            }
        });
    }
    
    private long getFileSize(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();
        
        long contentLength = connection.getContentLengthLong();
        connection.disconnect();
        return contentLength;
    }
    
    private boolean downloadWithResume(
            String url, 
            Path destinationPath, 
            long startByte, 
            DownloadProgress progress,
            Consumer<DownloadProgress> progressCallback) throws IOException {
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        
        if (startByte > 0) {
            connection.setRequestProperty("Range", "bytes=" + startByte + "-");
        }
        
        connection.connect();
        
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK && 
            responseCode != HttpURLConnection.HTTP_PARTIAL) {
            log.error("Server returned HTTP response code: {} for URL: {}", responseCode, url);
            return false;
        }
        
        try (InputStream inputStream = connection.getInputStream();
             BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
             FileOutputStream fileOutput = new FileOutputStream(destinationPath.toFile(), startByte > 0);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput)) {
            
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            
            progress.setStatus("Downloading");
            
            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                if (progress.isCancelled()) {
                    progress.setStatus("Cancelled");
                    return false;
                }
                
                bufferedOutput.write(buffer, 0, bytesRead);
                progress.getDownloadedBytes().addAndGet(bytesRead);
                
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
            }
            
            bufferedOutput.flush();
            return true;
            
        } finally {
            connection.disconnect();
        }
    }
    
    private boolean downloadSequential(
            String url, 
            Path destinationPath, 
            String expectedChecksum,
            Consumer<DownloadProgress> progressCallback) throws IOException {
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        
        long fileSize = connection.getContentLengthLong();
        DownloadProgress progress = new DownloadProgress(fileSize > 0 ? fileSize : -1);
        
        try (InputStream inputStream = connection.getInputStream();
             BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
             FileOutputStream fileOutput = new FileOutputStream(destinationPath.toFile());
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutput)) {
            
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            
            progress.setStatus("Downloading");
            
            while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                if (progress.isCancelled()) {
                    progress.setStatus("Cancelled");
                    return false;
                }
                
                bufferedOutput.write(buffer, 0, bytesRead);
                progress.getDownloadedBytes().addAndGet(bytesRead);
                
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
            }
            
            return true;
            
        } finally {
            connection.disconnect();
        }
    }
    
    private String calculateFileChecksum(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            try (InputStream inputStream = Files.newInputStream(filePath);
                 BufferedInputStream bufferedInput = new BufferedInputStream(inputStream)) {
                
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int bytesRead;
                
                while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            StringBuilder result = new StringBuilder();
            for (byte b : digest.digest()) {
                result.append(String.format("%02x", b));
            }
            
            return result.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to calculate checksum for {}: {}", filePath, e.getMessage());
            return null;
        }
    }
}