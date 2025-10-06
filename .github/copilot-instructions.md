# A4Sync AI Coding Agent Instructions

## Project Overview

A4Sync is a military-grade mod synchronization tool for Arma 4 built as a Maven multi-module Java 21 project with Spring Boot server, JavaFX client, and CLI tools. The core innovation is chunked downloads with resume capability for reliable deployment.

## Architecture & Module Boundaries

### Multi-Module Structure
- **a4sync-common**: Shared models (`ModSet`, `A4SyncConfig`, `ModIndex`) and utilities
- **a4sync-server**: Spring Boot REST API serving mod files with range request support
- **a4sync-client**: JavaFX desktop application with FXML controllers 
- **a4sync-tools**: Picocli-based CLI tools for repository management

### Key Service Boundaries
- `ChunkedDownloadService`: Handles resumable downloads with 1MB chunks and SHA-256 verification
- `RepositoryManager`: Multi-repository state management with async health checks
- `ModManager`: Local mod installation and hash verification
- `ModSetService`: Server-side mod set and file serving with range request support

### Data Flow Pattern
1. **Repository Discovery**: Auto-detect `.a4sync` config files at common paths (`/a4sync.json`, `/.a4sync`)
2. **Mod Synchronization**: Compare local vs remote mod hashes → download missing chunks → verify integrity
3. **Range Requests**: Server supports HTTP Range headers for resumable downloads (`ModController.downloadMod()`)

## Configuration System

### Client Configuration (Portable vs Standard Mode)
```java
// Priority order in ClientConfig.loadConfig():
// 1. Working directory (portable mode): ./a4sync-config.json  
// 2. User home (standard mode): ~/.a4sync/a4sync-config.json
// 3. Create new default config
```

### Server Configuration
Uses Spring Boot properties with custom `@ConfigurationProperties(prefix = "a4sync")`:
- `a4sync.root-directory`: Base path for mod storage
- `a4sync.authentication-enabled`: BCrypt password authentication
- Repository structure: `{root-directory}/{modset-name}/` contains mod files

### A4SyncConfig Format
Custom `.a4sync` JSON format replaces legacy `.a3s` standard with metadata, connection settings, and client preferences for auto-discovery.

## Development Workflows

### Build & Test
```bash
# Full build (requires Java 21)
mvn clean package

# Run specific module  
mvn -pl a4sync-server spring-boot:run
java -jar a4sync-client/target/a4sync-client-*.jar
java -jar a4sync-tools/target/a4sync-tools-*.jar repo status

# Docker deployment
docker-compose up  # Uses ./mods and ./config volumes
```

### CLI Tools Setup
```bash
# Enable tab completion (repository management workflow)
./scripts/setup-completion.sh --jar-path a4sync-tools.jar
java -jar a4sync-tools.jar repo init /path/to/repo
java -jar a4sync-tools.jar config init  # Creates ~/.a4sync/config.properties
```

## Critical Implementation Patterns

### Chunked Downloads with Resume
- Files split into 1MB chunks with individual SHA-256 hashes
- `LocalModRepository.getOutdatedChunks()` identifies delta updates needed
- HTTP Range requests enable partial downloads from arbitrary byte positions
- Progress tracking via `DownloadProgress` with speed/ETA calculations

### Configuration Loading Priority
Client config follows working-directory-first pattern for portable deployments. Server auto-generates A4SyncConfig at `/api/v1/config` endpoint for easy client discovery.

### Multi-Repository Support  
`MultiRepositoryService` manages multiple concurrent repository connections with health monitoring and automatic failover between mirror URLs.

### Error Handling Conventions
- Use CompletableFuture for async operations with proper exception chaining
- JavaFX Platform.runLater() for UI thread safety in controllers
- Lombok @Slf4j for consistent logging across all services

## Integration Points

- **Spring Boot Actuator**: Health checks at `/actuator/health` (used in Docker healthcheck)
- **OpenAPI/Swagger**: Auto-generated API docs for server endpoints
- **Range Request Support**: Essential for chunked downloads - implement in all file serving endpoints
- **BCrypt Authentication**: Server uses `ModProperties.repositoryPasswordHash` for secure auth

## File Organization Conventions

- FXML files in `src/main/resources/fxml/` with matching controller classes
- Configuration classes use `@ConfigurationProperties` with validation
- Service classes are stateless except for explicit repository managers
- Common models in `a4sync-common` to avoid circular dependencies between modules