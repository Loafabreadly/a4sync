package com.a4sync.server.service;

import com.a4sync.common.model.ModIndex;
import com.a4sync.common.model.ModSet;
import com.a4sync.server.config.ModProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModManagementService {
    private final ModProperties modProperties;
    private final ObjectMapper objectMapper;
    
    public int scanAndUpdateMods() {
        Path rootPath = Path.of(modProperties.getRootDirectory());
        int count = 0;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath, "@*")) {
            for (Path modPath : stream) {
                if (Files.isDirectory(modPath)) {
                    updateModIndex(modPath.getFileName().toString());
                    count++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan mods directory", e);
        }
        
        return count;
    }
    
    public ModSet createModSet(ModSet modSet) {
        Path modSetsDir = Path.of(modProperties.getRootDirectory(), "modsets");
        try {
            Files.createDirectories(modSetsDir);
            Path modSetPath = modSetsDir.resolve(modSet.getName() + ".json");
            objectMapper.writeValue(modSetPath.toFile(), modSet);
            return modSet;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create mod set", e);
        }
    }
    
    public ModIndex updateModIndex(String modName) {
        Path modPath = Path.of(modProperties.getRootDirectory(), modName);
        if (!Files.isDirectory(modPath)) {
            throw new IllegalArgumentException("Mod directory not found: " + modName);
        }
        
        try {
            ModIndex index = new ModIndex();
            index.setName(modName);
            index.setFiles(new ArrayList<>());
            
            // Calculate total size and collect files
            try (Stream<Path> walk = Files.walk(modPath)) {
                walk.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            ModIndex.ModFile modFile = new ModIndex.ModFile();
                            modFile.setPath(modPath.relativize(file).toString());
                            modFile.setSize(Files.size(file));
                            modFile.setHash(calculateFileHash(file));
                            index.getFiles().add(modFile);
                        } catch (IOException e) {
                            log.error("Failed to process file: " + file, e);
                        }
                    });
            }
            
            // Set total size
            index.setTotalSize(index.getFiles().stream()
                .mapToLong(ModIndex.ModFile::getSize)
                .sum());
            
            // Save index
            Path indexPath = modPath.resolve("mod.json");
            objectMapper.writeValue(indexPath.toFile(), index);
            
            return index;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update mod index", e);
        }
    }
    
    private String calculateFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
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