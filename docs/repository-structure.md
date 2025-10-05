# Repository Structure

This document details the A4Sync repository structure and file formats.

## Directory Layout

```
repository/
├── repository.properties      # Repository configuration
├── @ModName1/               # Mod directory (must start with @)
│   ├── mod.json            # Mod metadata
│   ├── addons/            # Mod PBOs
│   │   ├── mod1_core.pbo
│   │   └── mod1_extras.pbo
│   └── keys/              # Mod signing keys
│       └── mod1.bikey
├── @ModName2/
│   └── ...
└── modsets/               # Mod set definitions
    ├── training.json
    └── operations.json
```

## File Formats

### repository.properties

```properties
# Basic Settings
repository.name=Unit Mods
repository.description=Official unit mod repository
repository.version=1.0

# Authentication
repository.auth.enabled=false
repository.auth.type=basic

# Performance
repository.max-chunk-size=52428800
repository.parallel-downloads=3
```

### mod.json

```json
{
  "name": "@ModName",
  "version": "1.2.0",
  "size": 1234567890,
  "lastUpdated": "2025-10-04T12:00:00Z",
  "files": [
    {
      "path": "addons/mod1_core.pbo",
      "size": 123456,
      "chunks": [
        {
          "offset": 0,
          "size": 52428800,
          "checksum": "sha256:abc..."
        }
      ]
    }
  ],
  "dependencies": [
    "@CBA_A4"
  ]
}
```

### modset.json

```json
{
  "name": "Training Mods",
  "description": "Required mods for training sessions",
  "lastUpdated": "2025-10-04T12:00:00Z",
  "mods": [
    {
      "name": "@CBA_A4",
      "version": "1.0.0"
    },
    {
      "name": "@ACE",
      "version": "2.0.0"
    }
  ],
  "dlcRequired": [
    "csla",
    "gm"
  ],
  "totalSize": 12345678900
}
```

## File Chunking

Large files are split into chunks for efficient downloads:

1. Default chunk size: 50MB
2. Each chunk has a SHA-256 checksum
3. Chunks can be downloaded in parallel
4. Interrupted downloads can be resumed

Example chunk calculation:
```python
chunk_size = 52428800  # 50MB
file_size = 157286400  # 150MB

chunks = [
    # Chunk 1: 0-50MB
    {
        "offset": 0,
        "size": 52428800,
        "checksum": "sha256:..."
    },
    # Chunk 2: 50-100MB
    {
        "offset": 52428800,
        "size": 52428800,
        "checksum": "sha256:..."
    },
    # Chunk 3: 100-150MB
    {
        "offset": 104857600,
        "size": 52428800,
        "checksum": "sha256:..."
    }
]
```

## Checksums

1. File-level checksums:
   - Algorithm: SHA-256
   - Covers entire file
   - Stored in mod.json

2. Chunk-level checksums:
   - Algorithm: SHA-256
   - Used for download verification
   - Enables partial file updates

## Version Control

### Mod Versioning

1. Semantic versioning (MAJOR.MINOR.PATCH)
2. Version stored in mod.json
3. Client compares versions for updates

### Repository Versioning

1. Repository version in properties
2. Used for compatibility checks
3. Clients check minimum supported version

## Security

### File Permissions

```bash
# Repository root
chmod 755 /repository

# Mod directories
chmod 755 /repository/@*

# Configuration files
chmod 644 /repository/repository.properties
chmod 644 /repository/modsets/*.json
```

### Authentication Files

If using authentication:
```properties
# users.properties
admin=$2a$10$...  # BCrypt hash
reader=$2a$10$... # BCrypt hash
```

## Optimization

### Directory Structure

- Use flat structure for mods
- Keep paths short for Windows compatibility
- Use consistent naming conventions

### File Organization

1. Group related files:
   ```
   @Mod/addons/    # PBO files
   @Mod/keys/      # Signature keys
   @Mod/extras/    # Optional content
   ```

2. Optimize for partial updates:
   - Split large files into chunks
   - Enable differential updates
   - Track file modifications

## Migration

### From Arma 3

1. Directory changes:
   - Update paths for Arma 4
   - Adjust file structure

2. Metadata updates:
   - Convert mod.cpp to mod.json
   - Update dependencies

3. Content validation:
   - Verify PBO compatibility
   - Check signature keys