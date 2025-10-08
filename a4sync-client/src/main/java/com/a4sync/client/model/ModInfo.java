package com.a4sync.client.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about a mod's structure and metadata
 */
@Data
public class ModInfo {
    private String name;
    private String version;
    private long totalSize;
    private int pboCount;
    private int bikeyCount;
    private boolean hasMod_cpp;
    private boolean hasAddonsFolder;
    private boolean hasKeysFolder;
    private List<String> requiredAddons = new ArrayList<>();
}