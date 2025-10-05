package com.a4sync.common.model;

import lombok.Data;

@Data
public class ModChunk {
    private String id;          // Unique chunk identifier
    private String modName;     // Parent mod name
    private String path;        // Relative path within mod
    private long offset;        // Chunk start position
    private long length;        // Chunk length
    private String hash;        // SHA-256 hash of chunk
    private boolean lastChunk;  // Whether this is the last chunk
}