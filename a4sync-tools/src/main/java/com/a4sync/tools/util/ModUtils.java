package com.a4sync.tools.util;

import com.a4sync.common.model.ModChunk;
import com.a4sync.common.model.ModIndex;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for mod management operations
 */
public class ModUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();
    
    private static final int DEFAULT_CHUNK_SIZE = 50 * 1024 * 1024; // 50MB
    
    /**
     * Creates a mod index for the given mod path
     */
    public static void createModIndex(Path modPath, String version) throws IOException {
        if (!Files.exists(modPath) || !Files.isDirectory(modPath)) {
            throw new IllegalArgumentException("Mod path does not exist or is not a directory: " + modPath);
        }
        
        String modName = modPath.getFileName().toString();
        if (!modName.startsWith("@")) {
            throw new IllegalArgumentException("Mod directory must start with @: " + modName);
        }
        
        System.out.println("Scanning mod directory: " + modPath);
        
        ModIndex modIndex = new ModIndex();
        modIndex.setName(modName);
        modIndex.setVersion(version);
        modIndex.setLastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        List<ModIndex.ModFile> files = new ArrayList<>();
        final long[] totalSize = {0}; // Use array to make it effectively final
        
        // Scan all files in the mod directory
        Files.walkFileTree(modPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isRegularFile()) {
                    Path relativePath = modPath.relativize(file);
                    long fileSize = attrs.size();
                    totalSize[0] += fileSize;
                    
                    ModIndex.ModFile modFile = new ModIndex.ModFile();
                    modFile.setPath(relativePath.toString().replace('\\', '/'));
                    modFile.setSize(fileSize);
                    modFile.setHash(calculateFileHash(file));
                    
                    // Create chunks for large files
                    if (fileSize > DEFAULT_CHUNK_SIZE) {
                        modFile.setChunks(createChunks(file, fileSize));
                    }
                    
                    files.add(modFile);
                    System.out.println("  Added: " + relativePath + " (" + formatSize(fileSize) + ")");
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        modIndex.setFiles(files);
        modIndex.setTotalSize(totalSize[0]);
        modIndex.setHash(calculateModHash(files));
        
        // Write mod.json file
        Path modJsonPath = modPath.resolve("mod.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(modJsonPath.toFile(), modIndex);
        
        System.out.println("Created mod.json for " + modName + " (v" + version + ")");
        System.out.println("  Total files: " + files.size());
        System.out.println("  Total size: " + formatSize(totalSize[0]));
        System.out.println("  Hash: " + modIndex.getHash());
    }
    
    /**
     * Updates the mod index for the given mod path
     */
    public static void updateModIndex(Path modPath) throws IOException {
        Path modJsonPath = modPath.resolve("mod.json");
        if (!Files.exists(modJsonPath)) {
            throw new IllegalArgumentException("No mod.json found in: " + modPath + ". Use 'mod create' first.");
        }
        
        ModIndex existingIndex = objectMapper.readValue(modJsonPath.toFile(), ModIndex.class);
        String version = existingIndex.getVersion();
        
        System.out.println("Updating existing mod index (v" + version + ")");
        createModIndex(modPath, version);
    }
    
    /**
     * Creates chunks for a large file
     */
    private static List<ModChunk> createChunks(Path file, long fileSize) {
        List<ModChunk> chunks = new ArrayList<>();
        long offset = 0;
        
        while (offset < fileSize) {
            long chunkSize = Math.min(DEFAULT_CHUNK_SIZE, fileSize - offset);
            
            ModChunk chunk = new ModChunk();
            chunk.setOffset(offset);
            chunk.setLength(chunkSize);
            chunk.setLastChunk(offset + chunkSize >= fileSize);
            // Note: We're not calculating chunk checksums here for performance
            // They can be calculated on-demand by the server
            
            chunks.add(chunk);
            offset += chunkSize;
        }
        
        return chunks;
    }
    
    /**
     * Calculates SHA-256 hash for a file
     */
    private static String calculateFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return "sha256:" + bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Calculates overall mod hash based on file hashes
     */
    private static String calculateModHash(List<ModIndex.ModFile> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Hash all file hashes together for a mod hash
            for (ModIndex.ModFile file : files) {
                digest.update(file.getHash().getBytes());
            }
            
            byte[] hash = digest.digest();
            return "sha256:" + bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Converts byte array to hex string
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Reads mod metadata from mod.json file
     */
    public static ModIndex readModIndex(Path modPath) throws IOException {
        Path modJsonPath = modPath.resolve("mod.json");
        if (!Files.exists(modJsonPath)) {
            throw new IllegalArgumentException("No mod.json found in: " + modPath + ". Use 'mod create' first.");
        }
        return objectMapper.readValue(modJsonPath.toFile(), ModIndex.class);
    }
    
    /**
     * Formats file size in human-readable format
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}