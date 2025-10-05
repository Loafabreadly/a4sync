# A4Sync

A4Sync is a modern, efficient mod synchronization tool for Arma 4, inspired by Arma 3 Sync. It provides a client-server architecture for managing and distributing mods with support for partial downloads and repository authentication.

## Features

- 📁 Efficient mod synchronization with partial downloads
- 🔄 Support for multiple mod sets
- 🔒 Optional repository authentication
- 📊 Checksum verification for mod integrity
- 🚀 HTTP range requests for resumable downloads
- 🐳 Docker support for easy deployment

## Project Structure

```
a4sync/
├── a4sync-client/    # JavaFX client application
├── a4sync-common/    # Shared models and utilities
└── a4sync-server/    # Spring Boot server application
```

## Quick Start

### Server Setup

1. Create a configuration file `application.properties` in the same directory as the server jar:
```properties
server.port=8080
a4sync.root-directory=/path/to/mods
a4sync.authentication-enabled=false
```

2. Run the server:
```bash
java -jar a4sync-server.jar
```

Note: The root directory must exist and be writable by the server process.

Or using Docker:
```bash
docker run -v /path/to/mods:/a4sync -p 8080:8080 a4sync/server
```

### Client Usage

1. Download the latest client release
2. Launch the application
3. Add a repository URL
4. Select and sync mods

## Building from Source

Prerequisites:
- Java 17 or later
- Maven 3.6 or later

Build the project:
```bash
mvn clean install
```

## Repository Authentication

A4Sync supports optional repository authentication. To enable it:

1. Generate a password hash:
```bash
java -cp a4sync-server.jar com.a4sync.server.util.PasswordUtils mypassword
```

2. Add to server configuration:
```properties
a4sync.authentication-enabled=true
a4sync.repository-password=<generated-hash>
```

## Mod Organization

Mods should be organized in subdirectories under the root directory:
```
/a4sync/
├── CUP/
│   ├── Weapons/
│   │   └── weapons_core.pbo
│   └── Units/
│       └── units_core.pbo
└── RHS/
    └── Core/
        └── rhs_main.pbo
```

Each top-level directory represents a mod set.

## Setting up a Local Repository

The server expects mods to be organized in a specific structure. Each mod should be in its own directory under the root path.
You can manage mods using either the provided shell script or REST API.

### Using the Shell Script

The `mod-tools.sh` script provides easy mod management:

```bash
# Create/update mod.json for a single mod
./mod-tools.sh mod "/mods/@CUP_Terrains"

# Create a modset
./mod-tools.sh modset "/mods" "tactical" "tac_ops" "Tactical Operations Modset"

# Update all mod.json files in a directory
./mod-tools.sh update-all "/mods"
```

### Using the REST API (when running in Docker)

For Docker deployments, use the admin API endpoints:

```bash
# Scan and update all mods
curl -X POST http://localhost:8080/api/v1/admin/mods/scan

# Create a new modset
curl -X POST http://localhost:8080/api/v1/admin/modsets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "tactical",
    "description": "Tactical Operations Modset",
    "gameOptions": {
      "profileName": "tac_ops",
      "noSplash": true
    },
    "mods": ["@CUP_Terrains", "@RHS_AFRF"]
  }'

# Update a specific mod's index
curl -X PUT http://localhost:8080/api/v1/admin/mods/@CUP_Terrains/index
```

### Repository Structure

The mods should be organized as follows:

```
/path/to/mods/
├── @CUP_Terrains/          # Mod folder (must start with @)
│   ├── addons/            # Contains PBO files
│   │   ├── cup_terrains_core.pbo
│   │   └── cup_terrains_maps.pbo
│   ├── keys/             # Contains bikeys
│   │   └── cup_terrains.bikey
│   └── mod.json          # Mod metadata
├── @RHS_AFRF/
│   ├── addons/
│   ├── keys/
│   └── mod.json
└── modsets/              # Contains mod set definitions
    ├── tactical.json     # Example mod set
    └── training.json     # Another mod set
```

### Mod Metadata (mod.json)
```json
{
  "name": "@CUP_Terrains",
  "version": "1.0.0",
  "size": 1572864000,    # Total size in bytes
  "hash": "sha256-..."   # SHA-256 hash of all files
}
```

### Mod Set Definition (modsets/tactical.json)
```json
{
  "name": "Tactical Operations",
  "description": "Tactical gameplay mods",
  "gameOptions": {
    "profileName": "tactical",
    "noSplash": true
  },
  "mods": [
    "@CUP_Terrains",
    "@RHS_AFRF"
  ]
}
```

## API Documentation

The server provides a Swagger UI for exploring the API. Access it at:
```
http://your-server:8080/swagger-ui.html
```

### Key Endpoints

- `GET /api/v1/health` - Check server health and authentication
- `GET /api/v1/modsets` - List all mod sets
- `GET /api/v1/modsets/{name}` - Get mod set details
- `GET /api/v1/chunks/{id}` - Download mod chunk (supports range requests)
- `GET /api/v1/autoconfig` - Get automatic configuration

### Authentication

All endpoints except `/health` require authentication if enabled in server configuration.
Authentication uses SHA-256 hashed passwords passed in the `X-Repository-Auth` header.

### Large File Handling

A4Sync handles large mods efficiently through:
1. Chunked downloads (configurable chunk size)
2. HTTP range requests for resume support
3. Parallel download capabilities
4. Checksum verification per chunk
5. Disk space verification before downloads

The client automatically:
- Splits large downloads into manageable chunks
- Verifies available disk space
- Supports download resumption
- Validates file integrity

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by the Arma 3 Sync project
- Built with Spring Boot and JavaFX
- Thanks to all contributors
