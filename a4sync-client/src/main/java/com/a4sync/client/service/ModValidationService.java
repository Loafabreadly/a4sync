package com.a4sync.client.service;

import com.a4sync.client.model.ModInfo;
import com.a4sync.client.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class ModValidationService {
    
    // Common Arma mod folder patterns
    private static final Pattern MOD_NAME_PATTERN = Pattern.compile("^@[a-zA-Z0-9_\\-]+$");
    private static final Pattern PBO_FILE_PATTERN = Pattern.compile(".*\\.pbo$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BIKEY_FILE_PATTERN = Pattern.compile(".*\\.bikey$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BISIGN_FILE_PATTERN = Pattern.compile(".*\\.bisign$", Pattern.CASE_INSENSITIVE);
    

    
    public ValidationResult validateModFolder(Path modPath) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ModInfo modInfo = new ModInfo();
        
        try {
            // Check if path exists and is a directory
            if (!Files.exists(modPath)) {
                errors.add("Mod folder does not exist: " + modPath);
                return new ValidationResult(false, errors, warnings, null);
            }
            
            if (!Files.isDirectory(modPath)) {
                errors.add("Path is not a directory: " + modPath);
                return new ValidationResult(false, errors, warnings, null);
            }
            
            // Validate mod folder name
            String folderName = modPath.getFileName().toString();
            modInfo.setName(folderName);
            
            if (!MOD_NAME_PATTERN.matcher(folderName).matches()) {
                warnings.add("Mod folder name should start with @ and contain only alphanumeric characters, underscores, and hyphens");
            }
            
            // Calculate total size
            modInfo.setTotalSize(calculateFolderSize(modPath));
            
            // Check for required folder structure
            validateFolderStructure(modPath, modInfo, errors, warnings);
            
            // Validate PBO files
            validatePboFiles(modPath, modInfo, errors, warnings);
            
            // Validate key files
            validateKeyFiles(modPath, modInfo, errors, warnings);
            
            // Check mod.cpp file
            validateModCpp(modPath, modInfo, warnings);
            
            boolean isValid = errors.isEmpty();
            return new ValidationResult(isValid, errors, warnings, modInfo);
            
        } catch (Exception e) {
            log.error("Error validating mod folder {}: {}", modPath, e.getMessage(), e);
            errors.add("Validation error: " + e.getMessage());
            return new ValidationResult(false, errors, warnings, modInfo);
        }
    }
    
    private void validateFolderStructure(Path modPath, ModInfo modInfo, List<String> errors, List<String> warnings) throws IOException {
        Path addonsPath = modPath.resolve("addons");
        Path keysPath = modPath.resolve("keys");
        
        if (Files.exists(addonsPath) && Files.isDirectory(addonsPath)) {
            modInfo.setHasAddonsFolder(true);
        } else {
            warnings.add("No 'addons' folder found - mod may not have any content");
        }
        
        if (Files.exists(keysPath) && Files.isDirectory(keysPath)) {
            modInfo.setHasKeysFolder(true);
        } else {
            warnings.add("No 'keys' folder found - mod signatures cannot be verified");
        }
        
        // Check for common problematic files/folders
        Path[] problematicPaths = {
                modPath.resolve("Thumbs.db"),
                modPath.resolve(".DS_Store"),
                modPath.resolve("desktop.ini")
        };
        
        for (Path path : problematicPaths) {
            if (Files.exists(path)) {
                warnings.add("Found system file that should be excluded: " + path.getFileName());
            }
        }
    }
    
    private void validatePboFiles(Path modPath, ModInfo modInfo, List<String> errors, List<String> warnings) throws IOException {
        Path addonsPath = modPath.resolve("addons");
        if (!Files.exists(addonsPath)) {
            return;
        }
        
        try (Stream<Path> files = Files.walk(addonsPath, 2)) {
            List<Path> pboFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> PBO_FILE_PATTERN.matcher(path.getFileName().toString()).matches())
                    .toList();
            
            modInfo.setPboCount(pboFiles.size());
            
            if (pboFiles.isEmpty()) {
                warnings.add("No PBO files found in addons folder");
            }
            
            // Check for PBO files outside addons folder (usually not recommended)
            try (Stream<Path> rootFiles = Files.list(modPath)) {
                boolean hasPboInRoot = rootFiles
                        .filter(Files::isRegularFile)
                        .anyMatch(path -> PBO_FILE_PATTERN.matcher(path.getFileName().toString()).matches());
                
                if (hasPboInRoot) {
                    warnings.add("PBO files found in mod root - should typically be in 'addons' folder");
                }
            }
            
            // Validate individual PBO files
            for (Path pboFile : pboFiles) {
                validatePboFile(pboFile, warnings);
            }
        }
    }
    
    private void validatePboFile(Path pboFile, List<String> warnings) {
        try {
            long size = Files.size(pboFile);
            String fileName = pboFile.getFileName().toString();
            
            // Check for extremely large PBO files (might indicate packing issues)
            if (size > 500 * 1024 * 1024) { // 500MB
                warnings.add("Very large PBO file detected: " + fileName + " (" + formatFileSize(size) + ")");
            }
            
            // Check for PBO files with problematic names
            if (fileName.contains(" ")) {
                warnings.add("PBO file name contains spaces: " + fileName);
            }
            
            if (!fileName.toLowerCase().equals(fileName)) {
                warnings.add("PBO file name contains uppercase letters: " + fileName);
            }
            
        } catch (IOException e) {
            warnings.add("Could not validate PBO file: " + pboFile.getFileName());
        }
    }
    
    private void validateKeyFiles(Path modPath, ModInfo modInfo, List<String> errors, List<String> warnings) throws IOException {
        Path keysPath = modPath.resolve("keys");
        if (!Files.exists(keysPath)) {
            return;
        }
        
        try (Stream<Path> files = Files.walk(keysPath)) {
            List<Path> keyFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> BIKEY_FILE_PATTERN.matcher(path.getFileName().toString()).matches())
                    .toList();
            
            modInfo.setBikeyCount(keyFiles.size());
            
            if (keyFiles.isEmpty()) {
                warnings.add("No .bikey files found in keys folder");
            }
            
            // Check for matching signatures
            Path addonsPath = modPath.resolve("addons");
            if (Files.exists(addonsPath)) {
                try (Stream<Path> signFiles = Files.walk(addonsPath)) {
                    long signCount = signFiles
                            .filter(Files::isRegularFile)
                            .filter(path -> BISIGN_FILE_PATTERN.matcher(path.getFileName().toString()).matches())
                            .count();
                    
                    if (keyFiles.size() > 0 && signCount == 0) {
                        warnings.add("Keys found but no signature files (.bisign) found");
                    }
                }
            }
        }
    }
    
    private void validateModCpp(Path modPath, ModInfo modInfo, List<String> warnings) {
        Path modCppPath = modPath.resolve("mod.cpp");
        
        if (Files.exists(modCppPath) && Files.isRegularFile(modCppPath)) {
            modInfo.setHasMod_cpp(true);
            
            try {
                // Basic mod.cpp validation could be expanded here
                long size = Files.size(modCppPath);
                if (size == 0) {
                    warnings.add("mod.cpp file is empty");
                }
                
                // Could parse mod.cpp for version info, dependencies, etc.
                
            } catch (IOException e) {
                warnings.add("Could not read mod.cpp file");
            }
        } else {
            warnings.add("No mod.cpp file found - mod metadata will not be available");
        }
    }
    
    private long calculateFolderSize(Path folderPath) throws IOException {
        try (Stream<Path> files = Files.walk(folderPath)) {
            return files
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        }
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    public boolean isValidModFolder(Path modPath) {
        return validateModFolder(modPath).isValid();
    }
}