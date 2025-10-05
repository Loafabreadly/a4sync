package com.a4sync.common.model;

import lombok.Data;
import java.util.List;

@Data
public class ModIndex {
    private String name;            // Mod name (e.g., @CUP_Terrains)
    private String version;         // Mod version
    private long totalSize;         // Total size in bytes
    private String hash;            // Overall mod hash
    private List<ModFile> files;    // Files in the mod
    
    @Data
    public static class ModFile {
        private String path;        // Relative path in mod
        private long size;          // File size in bytes
        private String hash;        // File hash
        private List<ModChunk> chunks; // File chunks for download
    }
}