package com.a4sync.server.service;

import com.a4sync.common.model.A4SyncConfig;
import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import com.a4sync.common.model.RepositoryInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ModSetService {
    private static final int BUFFER_SIZE = 8192;
    private final Path rootPath;
    public ModSetService(Path rootPath, ObjectMapper modSetObjectMapper) {
        this.rootPath = rootPath;
        initializeRootPath();
    }

    private void initializeRootPath() {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create root directory", e);
        }
    }

    public List<ModSet> getAllModSets() {
        try {
            return Files.list(rootPath)
                .filter(Files::isDirectory)
                .filter(path -> !path.getFileName().toString().startsWith("."))
                .map(this::createModSetFromPath)
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list mod sets", e);
        }
    }

    public Optional<ModSet> getModSet(String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        Path modSetPath = rootPath.resolve(name);
        if (!Files.isDirectory(modSetPath)) {
            return Optional.empty();
        }
        return Optional.of(createModSetFromPath(modSetPath));
    }

    private ModSet createModSetFromPath(Path path) {
        ModSet modSet = new ModSet();
        modSet.setName(path.getFileName().toString());
        
        try {
            List<Mod> mods = Files.list(path)
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith("@"))
                .map(this::createModFromDirectoryPath)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
            modSet.setMods(mods);
            
            // Calculate total size from all mods
            long totalSize = mods.stream().mapToLong(Mod::getSize).sum();
            modSet.setTotalSize(totalSize);
            
            // Set last updated time based on directory modification time
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                LocalDateTime lastModified = LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(), 
                    ZoneId.systemDefault()
                );
                modSet.setLastUpdated(lastModified);
            } catch (IOException e) {
                // Default to current time if we can't get file attributes
                modSet.setLastUpdated(LocalDateTime.now());
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read mods from " + path, e);
        }
        return modSet;
    }

    public Path getModPath(String modSetName, String modName) {
        String sanitizedSet = StringUtils.cleanPath(modSetName);
        String sanitizedMod = StringUtils.cleanPath(modName);
        if (sanitizedSet.contains("..") || sanitizedMod.contains("..")) {
            throw new IllegalArgumentException("Invalid path");
        }
        return rootPath.resolve(sanitizedSet).resolve(sanitizedMod);
    }

    public ModSet generateAutoConfig() {
        return createModSetFromPath(rootPath);
    }
    
    public RepositoryInfo generateRepositoryInfo() {
        RepositoryInfo repoInfo = new RepositoryInfo();
        repoInfo.setName("A4Sync Repository");
        repoInfo.setLastUpdated(LocalDateTime.now());
        
        // Get modset names and count with size information
        List<ModSet> allModSets = getAllModSets();
        List<ModSet> modSetSummaries = allModSets.stream()
            .map(modSet -> {
                ModSet summary = new ModSet();
                summary.setName(modSet.getName());
                summary.setDescription(modSet.getDescription());
                summary.setVersion(modSet.getVersion());
                summary.setLastUpdated(modSet.getLastUpdated());
                
                // Calculate modset size from its mods
                if (modSet.getMods() != null) {
                    long totalSize = modSet.getMods().stream()
                        .mapToLong(mod -> mod.getSize())
                        .sum();
                    summary.setTotalSize(totalSize);
                }
                
                // Don't include full mods list - just the basic info for selection
                return summary;
            })
            .collect(Collectors.toList());
        
        repoInfo.setModSets(modSetSummaries);
        repoInfo.setModSetCount(allModSets.size());
        
        return repoInfo;
    }
    
    public A4SyncConfig generateA4SyncConfig(jakarta.servlet.http.HttpServletRequest request) {
        // Generate base URL from request
        String baseUrl = request.getScheme() + "://" + 
                        request.getServerName() + 
                        (request.getServerPort() != 80 && request.getServerPort() != 443 ? 
                         ":" + request.getServerPort() : "") + 
                        request.getContextPath() + "/api/v1";
        
        A4SyncConfig config = A4SyncConfig.createDefault("A4Sync Repository", baseUrl);
        
        // Update with actual repository information
        config.getRepository().setName("A4Sync Server Repository");
        config.getRepository().setDescription("Auto-generated A4Sync repository configuration");
        config.getRepository().setMaintainer("A4Sync Administrator");
        config.getRepository().setLastUpdated(LocalDateTime.now());
        
        return config;
    }

    private Optional<Mod> createModFromDirectoryPath(Path modDirectoryPath) {
        try {
            Mod mod = new Mod();
            mod.setName(modDirectoryPath.getFileName().toString());
            
            // Calculate total size and combined hash for all files in the mod directory
            List<Path> allFiles = Files.walk(modDirectoryPath)
                .filter(p -> !Files.isDirectory(p))
                .collect(Collectors.toList());
            
            long totalSize = 0;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Sort files by path to ensure consistent hash calculation
            allFiles.sort(Comparator.comparing(Path::toString));
            
            for (Path file : allFiles) {
                totalSize += Files.size(file);
                // Add file path to hash for structure consistency
                digest.update(file.getFileName().toString().getBytes());
                // Add file content to hash
                try (InputStream is = Files.newInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            
            mod.setSize(totalSize);
            
            // Convert hash to hex string
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            mod.setHash(hexString.toString());
            
            // Set relative path from root as version
            String relativePath = rootPath.relativize(modDirectoryPath.getParent()).toString();
            mod.setVersion(relativePath);
            return Optional.of(mod);
        } catch (IOException | NoSuchAlgorithmException e) {
            return Optional.empty();
        }
    }

    public long calculateTotalRepositorySize() {
        try {
            return Files.walk(rootPath)
                .filter(p -> !Files.isDirectory(p))
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        log.warn("Could not get size of file: {}", path, e);
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            log.error("Failed to calculate repository size", e);
            return 0;
        }
    }

}
