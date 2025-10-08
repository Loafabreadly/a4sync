# A4Sync Design Document

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Component Details](#component-details)
4. [Data Models](#data-models)
5. [Communication Protocols](#communication-protocols)
6. [Security & Authentication](#security--authentication)
7. [Performance & Scalability](#performance--scalability)
8. [Deployment](#deployment)
9. [Future Considerations](#future-considerations)

## System Overview

A4Sync is a comprehensive mod synchronization platform designed for military units and gaming communities using Arma 3/4. The system provides reliable, resumable mod downloads with integrity verification, multi-repository support, and seamless Steam integration.

### Key Features
- **Chunked Downloads**: 1MB chunks with SHA-256 verification and resume capability
- **Multi-Repository**: Support for multiple mod repositories with health monitoring and failover
- **Steam Integration**: Direct game launching with automatic mod parameter construction
- **Discord Integration**: Rich webhook notifications for modset updates
- **CLI Tools**: Complete repository management via command-line interface
- **Cross-Platform**: Java-based solution supporting Windows, Linux, and macOS

### Target Users
- Military simulation units requiring reliable mod distribution
- Gaming communities managing large mod collections
- Server administrators deploying mod repositories
- Content creators distributing custom modifications

## Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   A4Sync Client │◄──►│   A4Sync Server │◄──►│  File Storage   │
│   (JavaFX GUI)  │    │ (Spring Boot)   │    │   (Filesystem)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         │              │ Discord Webhook │              │
         │              │   Integration   │              │
         │              └─────────────────┘              │
         │                                               │
┌─────────────────┐                              ┌─────────────────┐
│   Steam Client  │                              │   A4Sync Tools  │
│   Integration   │                              │  (CLI Commands) │
└─────────────────┘                              └─────────────────┘
```

### Module Structure

#### a4sync-common
**Purpose**: Shared data models and utilities across all modules
**Key Components**:
- `ModSet`: Container for related mods with metadata
- `Mod`: Individual mod information with download details
- `A4SyncConfig`: Repository configuration format
- `RepositoryInfo`: Server metadata and statistics
- `GameOptions`: Game launch configuration
- `ModIndex`: Chunked file metadata for resumable downloads

#### a4sync-server
**Purpose**: Read-only REST API server for mod distribution and repository information
**Philosophy**: The server focuses solely on serving files and providing repository metadata. Administrative operations (creating modsets, managing mods) are handled via CLI tools to maintain clear separation of concerns and security.
**Key Components**:
- `ModController`: HTTP endpoints for mod/modset operations
- `ModSetService`: Business logic for repository management
- `DiscordWebhookService`: Notification system integration
- `HealthService`: System monitoring and status reporting
- Range request support for resumable downloads

#### a4sync-client
**Purpose**: Desktop GUI application for mod management
**Key Components**:
- `MainController`: Primary UI controller with repository management
- `MultiRepositoryService`: Multi-server connection management
- `ChunkedDownloadService`: Resumable download implementation
- `GameLauncher`: Steam integration and process management
- `ModManager`: Local mod installation and verification

#### a4sync-tools
**Purpose**: Command-line tools for repository administration
**Key Components**:
- `ModCommand`: Mod creation and management
- `ModSetCommand`: Modset operations
- `RepoCommand`: Repository initialization and validation
- `ConfigManager`: CLI configuration management

## Component Details

### Server Components

#### ModSetService
**Responsibilities**:
- Scan filesystem for available modsets
- Generate modset metadata and file listings
- Calculate repository statistics (size, mod count)
- Create A4Sync configuration for client auto-discovery
- Handle mod file access and validation

**Key Methods**:
```java
public List<ModSet> getAllModSets()
public Optional<ModSet> getModSet(String name)
public Path getModPath(String modSetName, String modName)
public A4SyncConfig generateA4SyncConfig(HttpServletRequest request)
public RepositoryInfo generateRepositoryInfo()
```

#### DiscordWebhookService
**Responsibilities**:
- Send rich embed notifications to Discord channels
- Format modset update information with before/after comparisons
- Handle webhook delivery failures with retry logic
- Support customizable appearance (colors, thumbnails, mentions)

**Configuration Options**:
- Webhook URL and authentication
- Custom bot username and avatar
- Embed color schemes and thumbnail images
- Mention configuration (@everyone, role mentions)

### Client Components

#### ChunkedDownloadService
**Responsibilities**:
- Download files in 1MB chunks with resume capability
- Verify SHA-256 checksums for integrity
- Provide real-time progress tracking with speed/ETA
- Handle network failures with automatic retry
- Support cancellation and pause/resume operations

**Download Process**:
1. Check if file exists locally with correct checksum
2. Query server for file size using HEAD request
3. Calculate chunk boundaries (1MB default)
4. Download chunks in parallel using HTTP Range requests
5. Verify each chunk's SHA-256 checksum
6. Assemble chunks into final file
7. Verify complete file checksum

#### MultiRepositoryService
**Responsibilities**:
- Manage connections to multiple A4Sync servers
- Monitor repository health and availability
- Provide failover to mirror URLs when primary fails
- Aggregate modsets from all configured repositories
- Handle per-repository authentication and configuration

#### GameLauncher
**Responsibilities**:
- Auto-detect Steam installation paths
- Construct game launch parameters with mod lists
- Support both Arma 3 and Arma 4 with proper App IDs
- Handle profile management and custom launch options
- Validate mod paths and availability before launch

**Launch Parameter Construction**:
```java
// Example: steam.exe -applaunch 107410 -mod="@CBA_A3;@ace" -nosplash -name="Unit Profile"
ProcessBuilder builder = new ProcessBuilder(
    steamPath + "/steam.exe",
    "-applaunch", gameType.getAppId(),
    "-mod=\"" + String.join(";", modPaths) + "\"",
    "-nosplash",
    "-name=" + profileName
);
```

## Data Models

### Core Models

#### ModSet
```java
@Data
public class ModSet {
    private String name;               // Display name
    private String description;        // Human-readable description
    private String version;           // Semantic version (1.0.0)
    private List<Mod> mods;          // Contained modifications
    private GameOptions gameOptions;  // Launch configuration
    private List<String> dlcRequired; // Required DLCs
    private long totalSize;          // Total download size
    private LocalDateTime lastUpdated; // Last modification time
}
```

#### Mod
```java
@Data
public class Mod {
    private String name;        // Mod identifier (@CBA_A3)
    private String version;     // Mod version
    private String downloadUrl; // Relative download path
    private String hash;        // SHA-256 checksum
    private long size;         // File size in bytes
}
```

#### A4SyncConfig
```java
@Data
public class A4SyncConfig {
    private String formatVersion = "1.0";
    private RepositoryMetadata repository;
    private ConnectionSettings connection;
    private ClientSettings client;
    
    @Data
    public static class RepositoryMetadata {
        private String name;
        private String description;
        private String maintainer;
        private String website;
        private List<String> supportedGames;
    }
    
    @Data
    public static class ConnectionSettings {
        private String baseUrl;
        private boolean requiresAuthentication;
        private List<String> mirrorUrls;
        private long timeoutMs;
        private int maxRetries;
    }
}
```

### Repository Structure

#### Server Filesystem Layout
```
/a4sync/                          # Root directory
├── Unit_Modset_Alpha/           # Modset directory
│   ├── @CBA_A3/                # Mod directory (Arma convention)
│   │   └── cba_a3.pbo          # Mod files
│   ├── @ace/
│   │   ├── ace_medical.pbo
│   │   └── ace_interact.pbo
│   └── modset.json             # Generated metadata
├── Unit_Modset_Bravo/
└── .a4sync                     # Repository configuration
```

#### Client Configuration
```json
{
  "repositories": [
    {
      "id": "uuid-string",
      "name": "Unit Repository",
      "url": "https://mods.unit.mil",
      "enabled": true,
      "useAuthentication": false,
      "healthStatus": "HEALTHY"
    }
  ],
  "steamPath": "C:/Program Files (x86)/Steam",
  "gamePath": "C:/Program Files (x86)/Steam/steamapps/common/Arma 3",
  "modDirectories": [
    "C:/Users/User/Documents/Arma 3/Mods"
  ],
  "defaultGameOptions": {
    "profileName": "Unit Profile",
    "gameType": "ARMA3",
    "noSplash": true,
    "additionalParameters": []
  }
}
```

## Communication Protocols

### HTTP API Endpoints

#### Modset Management
- `GET /api/v1/modsets` - List all available modsets
- `GET /api/v1/modsets/{name}` - Get specific modset details
- `GET /api/v1/modsets/{modset}/mods/{mod}` - Download mod file

#### Repository Information
- `GET /api/v1/repository/info` - Repository metadata and statistics
- `GET /api/v1/repository/size` - Total repository size
- `GET /api/v1/health` - Health check endpoint

#### Configuration Auto-Discovery
- `GET /a4sync.json` - A4Sync configuration (primary)
- `GET /.a4sync` - Alternative configuration endpoint
- `GET /config` - Legacy configuration endpoint

#### Version Compatibility
- `GET /api/v1/version` - Server version information

### HTTP Range Requests

A4Sync uses HTTP Range requests to enable resumable downloads:

```http
GET /api/v1/modsets/Alpha/@CBA_A3 HTTP/1.1
Host: mods.unit.mil
Range: bytes=1048576-2097151
Authorization: Basic dXNlcjpwYXNz

HTTP/1.1 206 Partial Content
Content-Range: bytes 1048576-2097151/15728640
Content-Length: 1048576
Accept-Ranges: bytes
```

### Discord Webhook Integration

Rich embed format for modset notifications:

```json
{
  "username": "A4Sync Server",
  "avatar_url": "https://unit.mil/logo.png",
  "embeds": [
    {
      "title": "📦 Modset Updated: Unit_Alpha",
      "description": "Modset has been updated with new content",
      "color": 5814783,
      "fields": [
        {
          "name": "📊 Statistics",
          "value": "**Mods:** 12 (+2)\n**Size:** 2.4 GB (+156 MB)",
          "inline": true
        },
        {
          "name": "➕ Added Mods",
          "value": "• @enhanced_movement\n• @task_force_radio",
          "inline": true
        }
      ],
      "thumbnail": {
        "url": "https://unit.mil/thumbnail.png"
      },
      "timestamp": "2025-10-07T15:30:00.000Z"
    }
  ]
}
```

## Security & Authentication

### Authentication Methods

#### Server Authentication
- **BCrypt Password Hashing**: Secure password storage with configurable rounds
- **HTTP Basic Authentication**: Standard authentication header support
- **Optional Authentication**: Can be disabled for public repositories

#### File Integrity
- **SHA-256 Checksums**: All files verified with cryptographic hashes
- **Chunk-Level Verification**: Each 1MB chunk independently verified
- **Tamper Detection**: Automatic re-download on checksum mismatch

### Security Considerations

#### Network Security
- HTTPS preferred for all communications
- Certificate validation for secure connections
- Rate limiting protection against abuse
- Request size limits to prevent DoS attacks

#### File System Security
- Path traversal protection in file serving
- Restricted access to repository root directory
- Validation of file names and paths
- Secure temporary file handling

## Performance & Scalability

### Download Performance

#### Chunked Download Benefits
- **Resume Capability**: Interrupted downloads can resume from last completed chunk
- **Parallel Downloads**: Multiple chunks downloaded simultaneously
- **Memory Efficiency**: 1MB chunks prevent large files from consuming excessive memory
- **Error Isolation**: Individual chunk failures don't affect entire download

#### Performance Optimizations
- **Connection Pooling**: HTTP client reuses connections for efficiency
- **Compression**: Server supports gzip compression for JSON responses
- **Caching**: Client caches repository metadata and health status
- **Lazy Loading**: Large modset collections loaded on demand

### Server Scalability

#### File Serving
- **Direct File Serving**: Files served directly from filesystem without application processing
- **Range Request Support**: Efficient partial content delivery
- **Static File Optimization**: Web server can handle static files directly
- **CDN Compatibility**: Standard HTTP semantics work with content delivery networks

#### Resource Usage
- **Minimal Memory Footprint**: File streaming without loading into memory
- **Efficient File Scanning**: NIO.2 for fast filesystem operations
- **Configurable Thread Pools**: Adjustable concurrency limits
- **Health Monitoring**: Spring Boot Actuator for runtime metrics

## Deployment

### Docker Deployment

#### Production Configuration
```yaml
version: '3.8'
services:
  a4sync-server:
    image: a4sync-server:latest
    ports:
      - "443:8080"
    volumes:
      - ./mods:/a4sync:ro          # Read-only mod storage
      - ./config:/config          # Configuration files
      - ./logs:/logs              # Log file persistence
    environment:
      - JAVA_OPTS=-Xmx4G -Xms1G   # JVM tuning
      - a4sync.root-directory=/a4sync
      - a4sync.authentication-enabled=true
      - a4sync.discord.enabled=true
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

#### Reverse Proxy Configuration (nginx)
```nginx
server {
    listen 443 ssl http2;
    server_name mods.unit.mil;
    
    ssl_certificate /etc/ssl/certs/unit.mil.crt;
    ssl_certificate_key /etc/ssl/private/unit.mil.key;
    
    client_max_body_size 10G;
    
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Static file serving for better performance
    location ~* ^/api/v1/modsets/.*/mods/ {
        proxy_pass http://localhost:8080;
        proxy_buffering off;
        proxy_request_buffering off;
    }
}
```

### Client Distribution

#### Executable JAR
- Self-contained JavaFX application with embedded JRE
- Automatic updates via integrated update checker
- Portable configuration support for USB deployment
- Cross-platform compatibility (Windows, Linux, macOS)

#### Installation Methods
- **Portable**: Extract and run from any directory
- **System Install**: Integration with system package managers
- **Steam Workshop**: Distribution through Steam community
- **Direct Download**: Download from repository web interface

## Future Considerations

### Planned Enhancements

#### P2P Distribution
- BitTorrent-style peer-to-peer distribution for large files
- Reduced server bandwidth requirements
- Improved download speeds through multiple sources
- Automatic seeding from clients with complete files

#### Advanced Repository Management
- Repository mirroring and synchronization tools
- Automated modset building from Steam Workshop
- Version control integration (Git-based mod tracking)
- Dependency resolution and conflict detection

#### Enhanced UI/UX
- Modern web-based client interface
- Mobile companion app for remote management
- Advanced filtering and search capabilities
- Modset comparison and diff visualization

#### Performance Improvements
- Delta synchronization for mod updates
- Compression algorithms for reduced transfer sizes
- Smart caching strategies with TTL management
- Predictive prefetching based on usage patterns

### Extensibility Points

#### Plugin System
- Custom authentication providers
- Alternative storage backends (S3, Azure Blob)
- Custom notification channels (Slack, Teams)
- Game integration beyond Arma series

#### API Extensibility
- GraphQL API for advanced querying
- WebSocket connections for real-time updates
- Webhook system for external integrations
- RESTful API versioning strategy

### Technical Debt & Maintenance

#### Code Quality
- Comprehensive test coverage with integration tests
- Static analysis integration (SonarQube, SpotBugs)
- Dependency vulnerability scanning
- Performance profiling and optimization

#### Documentation
- API documentation with OpenAPI/Swagger
- User guides and administrator manuals
- Video tutorials for common workflows
- Community contribution guidelines

#### Monitoring & Observability
- Distributed tracing with OpenTelemetry
- Metrics collection with Prometheus
- Log aggregation with structured logging
- Performance dashboards and alerting

---

*This design document represents the current architecture and planned improvements for A4Sync. It should be updated as the system evolves and new requirements emerge.*