# A4Sync AI Coding Agent Instructions

## Project Overview

A4Sync is a military-grade mod synchronization tool for Arma 3/4 built as a Maven multi-module Java 21 project. It provides chunked downloads with resume capability, multi-repository support, Steam integration, Discord notifications, and comprehensive CLI tools for reliable military unit deployment.

## Architecture & Module Boundaries

### Multi-Module Structure
- **a4sync-common**: Shared models (`ModSet`, `Mod`, `A4SyncConfig`, `RepositoryInfo`, `GameOptions`, `ModIndex`), enums (`GameType`, `HealthStatus`), and version utilities
- **a4sync-server**: Spring Boot REST API with mod file serving, health monitoring, Discord webhooks, and A4Sync config generation
- **a4sync-client**: JavaFX desktop application with multi-repository management, Steam integration, and consolidated UI
- **a4sync-tools**: Picocli CLI tools for repository management, mod creation, and modset administration

### Key Service Boundaries

#### Server Services (`a4sync-server`)
- `ModSetService`: Repository discovery, modset management, A4Sync config generation, and file size calculations
- `DiscordWebhookService`: Rich Discord embed notifications for modset updates with customizable appearance
- `HealthService`: System health monitoring and status reporting via Spring Boot Actuator

#### Client Services (`a4sync-client`)  
- `ChunkedDownloadService`: Resumable downloads with 1MB chunks, SHA-256 verification, progress tracking, and retry logic
- `RepositoryService`: Single repository connection management with authentication, health checks, and version compatibility
- `MultiRepositoryService`: Multi-repository aggregation, health monitoring, and failover management
- `ModManager`: Local mod installation, validation, and integrity verification using enhanced ChunkedDownloadService
- `GameLauncher`: Steam integration for Arma 3/4 launching with mod parameter construction
- `A4SyncConfigService`: Auto-discovery and parsing of `.a4sync` configuration files from multiple sources

### Data Flow Pattern
1. **Repository Discovery**: Client auto-discovers repositories via `.a4sync` files at `/a4sync.json`, `/.a4sync`, `/config` or manual configuration
2. **Health Monitoring**: Async health checks via `/api/v1/health` endpoint with status caching and retry logic
3. **Mod Synchronization**: 
   - Query `/api/v1/modsets` for available modsets
   - Compare local vs remote mod versions and SHA-256 hashes
   - Download missing/updated files via `/api/v1/modsets/{modset}/mods/{mod}` with HTTP Range support
   - Verify integrity with SHA-256 checksums and resume on failure
4. **Steam Integration**: Launch games via `steam.exe -applaunch {APP_ID} -mod="@mod1;@mod2" -nosplash -name={profile}`
5. **Discord Notifications**: Server sends rich embeds to Discord webhooks on modset updates with thumbnails and color coding

## Configuration System

### Client Configuration (Portable vs Standard Mode)
```java
// Priority order in ClientConfig.loadConfig():
// 1. Working directory (portable mode): ./a4sync-client-config.json  
// 2. User home (standard mode): ~/.a4sync/a4sync-client-config.json
// 3. Create new default config with multi-repository support
```

### Server Configuration
Uses Spring Boot properties with custom `@ConfigurationProperties(prefix = "a4sync")`:
- `a4sync.root-directory`: Base path for mod storage (`/a4sync` default)
- `a4sync.authentication-enabled`: BCrypt password authentication (disabled by default)
- `a4sync.discord.enabled`: Discord webhook notifications (disabled by default)
- Repository structure: `{root-directory}/{modset-name}/@{mod-name}/` contains mod files

### A4SyncConfig Format
Custom `.a4sync` JSON format with comprehensive metadata:
```json
{
  "formatVersion": "1.0",
  "repository": {
    "name": "Unit Repository",
    "description": "Military unit mod collection",
    "supportedGames": ["arma3", "arma4"]
  },
  "connection": {
    "baseUrl": "https://mods.unit.mil",
    "requiresAuthentication": false,
    "mirrorUrls": ["https://backup.unit.mil"]
  },
  "client": {
    "download": {
      "chunkSize": 1048576,
      "maxParallelDownloads": 4,
      "verifyChecksums": true
    }
  }
}
```

## Development Workflows

### Build & Test
```bash
# Full build (requires Java 21)
mvn clean package

# Run specific module  
mvn -pl a4sync-server spring-boot:run
java -jar a4sync-client/target/a4sync-client-*.jar
java -jar a4sync-tools/target/a4sync-tools-*.jar --help

# Docker deployment
docker-compose up  # Uses ./mods and ./config volumes with health checks
```

### CLI Tools Setup
```bash
# Enable shell completion
java -jar a4sync-tools.jar completion bash > ~/.a4sync-completion
source ~/.a4sync-completion

# Repository management workflow
java -jar a4sync-tools.jar config init  # Creates ~/.a4sync/config.properties
java -jar a4sync-tools.jar repo init /path/to/repo
java -jar a4sync-tools.jar modset create "Unit Modset"
java -jar a4sync-tools.jar mod add "@CBA_A3" --modset "Unit Modset"
```

## Critical Implementation Patterns

### Chunked Downloads with Resume
- Files split into 1MB chunks with individual SHA-256 hashes stored in `ModIndex`
- HTTP Range requests (`Range: bytes=start-end`) enable partial downloads from arbitrary byte positions
- Progress tracking via `DownloadProgress` with real-time speed/ETA calculations and cancellation support
- Automatic retry logic with exponential backoff for network failures
- Integrity verification using SHA-256 checksums with automatic re-download on mismatch

### Multi-Repository Architecture
- `MultiRepositoryService` manages concurrent connections to multiple A4Sync servers
- Health monitoring with async status checks and automatic failover to mirror URLs
- Repository-specific authentication and configuration isolation
- Consolidated view in JavaFX UI with per-repository status indicators

### Steam Game Integration
- Auto-detection of Steam installation paths across platforms
- Game type support for both Arma 3 (`GameType.ARMA3`, App ID 107410) and Arma 4 (`GameType.ARMA4`)
- Mod parameter construction: `-mod="@mod1;@mod2;@mod3"` format
- Profile and launch option management through `GameOptions` configuration

### Discord Webhook Integration
- Rich embed notifications with customizable colors, thumbnails, and branding
- Modset update notifications with before/after mod lists and size changes
- Configurable mention support (`@everyone`, role mentions, custom text)
- Error handling and retry logic for webhook delivery failures

### Configuration Loading Priority
Client config follows working-directory-first pattern for portable deployments. Server auto-generates A4SyncConfig at multiple endpoints (`/a4sync.json`, `/.a4sync`, `/config`) for easy client discovery and backward compatibility.

### Error Handling Conventions
- Use CompletableFuture for async operations with proper exception chaining
- JavaFX Platform.runLater() for UI thread safety in controllers
- Lombok @Slf4j for consistent logging across all services
- Custom exceptions: `AuthenticationFailedException`, `RateLimitExceededException`
- Graceful degradation with user-friendly error messages

## Integration Points

### Server API Endpoints
- **GET /api/v1/modsets** - List all available modsets
- **GET /api/v1/modsets/{name}** - Get specific modset details
- **GET /api/v1/modsets/{modset}/mods/{mod}** - Download mod files (supports Range requests)
- **GET /api/v1/repository/info** - Repository metadata and statistics
- **GET /api/v1/health** - Health check endpoint (Spring Boot Actuator)
- **GET /api/v1/version** - Version compatibility information
- **GET /{/a4sync.json,/.a4sync,/config}** - A4Sync configuration auto-discovery

### External Integrations
- **Spring Boot Actuator**: Health checks at `/actuator/health` (used in Docker healthcheck)
- **OpenAPI/Swagger**: Auto-generated API documentation at `/swagger-ui/`
- **Discord Webhooks**: Rich embed notifications with retry logic and rate limiting
- **Steam Integration**: Process launching via `ProcessBuilder` with proper argument escaping
- **File System**: Cross-platform path handling with Java NIO for mod storage and scanning

## File Organization Conventions

### Source Structure
- FXML files in `src/main/resources/fxml/` with matching controller classes in `controller/` package
- Service classes in `service/` package with clear separation of concerns
- Configuration classes use `@ConfigurationProperties` with Jackson serialization
- Common models in `a4sync-common` to avoid circular dependencies between modules
- Exception classes in dedicated `exception/` package with specific error types

### Runtime Structure
- Server mod storage: `{root-directory}/{modset-name}/@{mod-name}/` (follows Arma convention)
- Client configuration: Working directory (portable) or `~/.a4sync/` (installed)
- CLI configuration: `~/.a4sync/config.properties` with repository defaults
- Log files: Application-specific locations with rotation support

## Security & Performance Considerations

### Authentication
- Optional BCrypt password hashing for repository access
- SHA-256 file integrity verification prevents tampering
- HTTP Basic authentication with secure header transmission
- Rate limiting protection against abuse

### Performance Optimizations
- Chunked downloads reduce memory usage and enable resume capability
- Parallel download support with configurable thread pools
- File system scanning optimizations with NIO.2 and parallel streams
- Connection pooling and keep-alive for HTTP clients
- Lazy loading of large modset collections

### Reliability
- Comprehensive error handling with automatic recovery
- Health monitoring and failover capabilities
- Integrity verification at multiple levels (file, chunk, checksum)
- Graceful degradation when repositories are unavailable
- Detailed logging for troubleshooting and audit trails