package com.a4sync.common.model;

import lombok.Data;
import java.util.List;

/**
 * Represents a file within a mod with chunk information for downloads
 */
@Data
public class ModFile {
    private String path;        // Relative path in mod
    private long size;          // File size in bytes
    private String hash;        // File hash
    private List<ModChunk> chunks; // File chunks for download
}