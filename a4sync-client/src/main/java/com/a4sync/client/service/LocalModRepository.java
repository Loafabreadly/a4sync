package com.a4sync.client.service;

import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LocalModRepository {
    private final ClientConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MOD_METADATA_FILE = "mod_metadata.json";
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    /**
     * Represents a mod file chunk for partial updates
     */
    private static class ModChunk {
        String hash;
        long offset;
        int size;
    }

    /**
     * Extended mod metadata for local storage
     */
    private static class LocalModMetadata {
        String name;
        String version;
        String fullHash;
        List<ModChunk> chunks;
        long lastUpdated;
        Map<String, String> additionalHashes; // For additional files within the mod
    }

    /**
     * Checks if a mod needs updating by comparing local and remote metadata
     */
    public boolean needsUpdate(Mod remoteMod) throws IOException {
        Path modPath = findModPath(remoteMod.getName());
        if (modPath == null) {
            return true;
        }

        LocalModMetadata localMeta = loadLocalMetadata(remoteMod.getName());
        if (localMeta == null) {
            return true;
        }

        // Check version and full hash first
        if (!localMeta.version.equals(remoteMod.getVersion()) || 
            !localMeta.fullHash.equals(remoteMod.getHash())) {
            return true;
        }

        return false;
    }

    /**
     * Gets the list of chunks that need to be updated
     */
    public List<Long> getOutdatedChunks(Mod remoteMod) throws IOException {
        Path modPath = findModPath(remoteMod.getName());
        if (modPath == null) {
            return Collections.emptyList();
        }

        LocalModMetadata localMeta = loadLocalMetadata(remoteMod.getName());
        if (localMeta == null) {
            return Collections.emptyList();
        }

        List<Long> outdatedChunks = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(modPath.toFile(), "r")) {
            byte[] buffer = new byte[CHUNK_SIZE];
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            for (int i = 0; i < localMeta.chunks.size(); i++) {
                ModChunk chunk = localMeta.chunks.get(i);
                file.seek(chunk.offset);
                int bytesRead = file.read(buffer, 0, chunk.size);
                if (bytesRead != chunk.size) {
                    outdatedChunks.add((long) i);
                    continue;
                }

                digest.reset();
                digest.update(buffer, 0, bytesRead);
                String chunkHash = bytesToHex(digest.digest());
                
                if (!chunkHash.equals(chunk.hash)) {
                    outdatedChunks.add((long) i);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to check chunks", e);
        }

        return outdatedChunks;
    }

    /**
     * Updates specific chunks of a mod
     */
    public CompletableFuture<Void> updateModChunks(Mod remoteMod, Map<Long, byte[]> chunks) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path modPath = findModPath(remoteMod.getName());
                if (modPath == null) {
                    modPath = getPreferredModPath(remoteMod.getName());
                }

                // Ensure parent directories exist
                Files.createDirectories(modPath.getParent());

                // Update chunks in the file
                try (RandomAccessFile file = new RandomAccessFile(modPath.toFile(), "rw")) {
                    for (Map.Entry<Long, byte[]> entry : chunks.entrySet()) {
                        long chunkIndex = entry.getKey();
                        byte[] chunkData = entry.getValue();
                        file.seek(chunkIndex * CHUNK_SIZE);
                        file.write(chunkData);
                    }
                }

                // Update local metadata
                updateLocalMetadata(remoteMod);
            } catch (Exception e) {
                throw new RuntimeException("Failed to update mod chunks", e);
            }
        });
    }

    private Path findModPath(String modName) {
        for (Path dir : config.getModDirectories()) {
            Path modPath = dir.resolve(modName);
            if (Files.exists(modPath)) {
                return modPath;
            }
        }
        return null;
    }

    private Path getPreferredModPath(String modName) {
        // Use the first configured mod directory
        return config.getModDirectories().get(0).resolve(modName);
    }

    private LocalModMetadata loadLocalMetadata(String modName) {
        Path modPath = findModPath(modName);
        if (modPath == null) return null;

        Path metaPath = modPath.getParent().resolve(MOD_METADATA_FILE);
        if (!Files.exists(metaPath)) return null;

        try {
            Map<String, LocalModMetadata> allMeta = objectMapper.readValue(metaPath.toFile(), 
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, LocalModMetadata.class));
            return allMeta.get(modName);
        } catch (IOException e) {
            return null;
        }
    }

    private void updateLocalMetadata(Mod remoteMod) throws IOException {
        Path modPath = findModPath(remoteMod.getName());
        if (modPath == null) return;

        Path metaPath = modPath.getParent().resolve(MOD_METADATA_FILE);
        Map<String, LocalModMetadata> allMeta = Files.exists(metaPath) ?
            objectMapper.readValue(metaPath.toFile(), 
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, LocalModMetadata.class)) :
            new HashMap<>();

        LocalModMetadata meta = new LocalModMetadata();
        meta.name = remoteMod.getName();
        meta.version = remoteMod.getVersion();
        meta.fullHash = remoteMod.getHash();
        meta.lastUpdated = System.currentTimeMillis();
        meta.chunks = calculateChunks(modPath);
        meta.additionalHashes = calculateAdditionalHashes(modPath);

        allMeta.put(remoteMod.getName(), meta);
        objectMapper.writeValue(metaPath.toFile(), allMeta);
    }

    private List<ModChunk> calculateChunks(Path modPath) throws IOException {
        List<ModChunk> chunks = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(modPath.toFile(), "r")) {
            byte[] buffer = new byte[CHUNK_SIZE];
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long offset = 0;

            while (true) {
                int bytesRead = file.read(buffer);
                if (bytesRead <= 0) break;

                digest.reset();
                digest.update(buffer, 0, bytesRead);
                
                ModChunk chunk = new ModChunk();
                chunk.hash = bytesToHex(digest.digest());
                chunk.offset = offset;
                chunk.size = bytesRead;
                chunks.add(chunk);

                offset += bytesRead;
            }
        } catch (Exception e) {
            throw new IOException("Failed to calculate chunks", e);
        }
        return chunks;
    }

    private Map<String, String> calculateAdditionalHashes(Path modPath) throws IOException {
        Map<String, String> hashes = new HashMap<>();
        if (!Files.isDirectory(modPath)) {
            return hashes;
        }

        try {
            Files.walk(modPath)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(Files.readAllBytes(file));
                        String relativePath = modPath.relativize(file).toString();
                        hashes.put(relativePath, bytesToHex(hash));
                    } catch (Exception e) {
                        // Skip files that can't be hashed
                    }
                });
        } catch (Exception e) {
            throw new IOException("Failed to calculate additional hashes", e);
        }
        return hashes;
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
