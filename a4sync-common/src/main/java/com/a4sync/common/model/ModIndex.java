package com.a4sync.common.model;

import lombok.Data;
import java.util.List;

@Data
public class ModIndex {
    private String name;            // Mod name (e.g., @CUP_Terrains)
    private String version;         // Mod version
    private long totalSize;         // Total size in bytes
    private String hash;            // Overall mod hash
    private String lastUpdated;     // When the mod was last updated (ISO format string)
    private List<ModFile> files;    // Files in the mod

}