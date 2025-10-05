# Repository Structure

This document details the A4Sync repository structure and file formats, including Phase 1 & 2 enhancements.

## Directory Layout

```
repository/
├── repository.properties      # Repository configuration
├── a4sync.json               # A4Sync configuration (Phase 2)
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

## New Configuration Formats (Phase 2)

### A4Sync Configuration (.a4sync / a4sync.json)

The new A4Sync configuration format provides enhanced repository metadata and client settings:

```json
{
  "repository": {
    "name": "Unit Mod Repository",
    "description": "Official military unit mods",
    "version": "1.0.0",
    "url": "https://mods.unit.com:8080",
    "lastUpdated": "2025-10-05T12:00:00Z"
  },
  "connection": {
    "protocol": "https",
    "host": "mods.unit.com",
    "port": 8080,
    "timeout": 30000,
    "retryAttempts": 3,
    "authRequired": false
  },
  "client": {
    "download": {
      "maxParallelDownloads": 3,
      "chunkSize": 52428800,
      "resumeSupported": true,
      "verifyChecksums": true
    },
    "security": {
      "allowInsecure": false,
      "validateCertificates": true,
      "minTlsVersion": "1.2"
    },
    "ui": {
      "showProgress": true,
      "showSpeed": true,
      "showEta": true,
      "autoRefresh": true
    }
  },
  "compatibility": {
    "minClientVersion": "1.0.0",
    "supportedFormats": ["a4sync", "json"],
    "features": ["chunked-download", "resume", "integrity-check"]
  }
}
```

## File Formats

### repository.properties

```properties
# Basic Settings
repository.name=Unit Mods
repository.description=Official unit mod repository
repository.version=1.0

# Authentication
a4sync.authentication-enabled=false
a4sync.repository-password=<bcrypt-hash>  # Generated using PasswordUtils

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

## Enhanced Download System (Phase 1)

### Chunked Downloads

Large files are automatically split into chunks for reliable downloads:

1. **Default chunk size**: 50MB (configurable)
2. **SHA-256 checksums**: Each chunk independently verified
3. **Parallel downloads**: Multiple chunks simultaneously 
4. **Resume capability**: Interrupted downloads automatically resume
5. **Progress tracking**: Real-time speed and ETA calculations
6. **Integrity assurance**: Multi-layer verification system

### File Validation

The enhanced validation system includes:

1. **Mod Structure Validation**: 
   - Verifies Arma mod folder structure
   - Checks for required PBO files in addons/
   - Validates key files in keys/ directory

2. **Content Integrity**:
   - SHA-256 checksum verification for all files
   - PBO file format validation
   - BIKEY signature verification

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

### Authentication

The repository uses a single password-based authentication system:

1. Server stores a BCrypt hash of the repository password in `application.properties`
2. Clients send a SHA-256 hash of their password in the `X-Repository-Auth` header
3. Server verifies the client's SHA-256 hash against its stored BCrypt hash

To set up authentication:
```bash
# Generate password hashes
java -cp a4sync-server.jar com.a4sync.server.util.PasswordUtils mypassword

# Add the BCrypt hash to application.properties
a4sync.authentication-enabled=true
a4sync.repository-password=<bcrypt-hash>
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