package com.a4sync.server.service;

import com.a4sync.common.model.Mod;
import com.a4sync.common.model.ModSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

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
            List<Mod> mods = Files.walk(path)
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> p.toString().endsWith(".pbo"))
                .map(this::createModFromPath)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
            modSet.setMods(mods);
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

    private Optional<Mod> createModFromPath(Path path) {
        try {
            Mod mod = new Mod();
            mod.setName(path.getFileName().toString());
            mod.setSize(Files.size(path));
            mod.setHash(calculateChecksum(path));
            // Set relative path from root as version
            String relativePath = rootPath.relativize(path.getParent()).toString();
            mod.setVersion(relativePath);
            return Optional.of(mod);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private String calculateChecksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
