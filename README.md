# A4Sync

A comprehensive, mod synchronization platform for Arma 3/4, featuring chunked downloads with resume capability, multi-repository support, Steam integration, and Discord notifications. Built for milsim units and gaming communities requiring reliable mod distribution.

## ✨ Features

### 🚀 Download Engine
- **Chunked Downloads**: Files split into 1MB chunks with SHA-256 verification
- **Resume Capability**: Interrupted downloads resume from last completed chunk  
- **Parallel Processing**: Multiple chunks downloaded simultaneously for speed
- **Progress Tracking**: Real-time speed, percentage, and ETA calculations
- **Error Recovery**: Automatic retry with exponential backoff

### 🌐 Multi-Repository Support
- **Repository Discovery**: Auto-detect `.a4sync` configuration files
- **Health Monitoring**: Async status checks with failover to mirror URLs
- **Per-Repository Auth**: Individual authentication settings per repository
- **Unified Interface**: Single UI managing multiple mod sources
- **Configuration Auto-Discovery**: Zero-config setup via standard endpoints

### 🎮 Steam Integration
- **Auto-Detection**: Finds Steam installation across Windows, Linux, macOS
- **Game Support**: Arma 3 (App ID 107410) and Arma 4 ready
- **Mod Loading**: Proper `-mod="@mod1;@mod2"` parameter construction
- **Profile Management**: Custom launch options and profile names
- **Path Validation**: Ensures mods exist before launching

### 📢 Discord Integration
- **Rich Notifications**: Embed notifications with thumbnails and custom colors
- **Modset Updates**: Before/after comparisons with mod lists and size changes
- **Configurable Mentions**: Support for @everyone, role mentions, custom text
- **Retry Logic**: Reliable webhook delivery with error handling

### 🛠️ CLI Tools
- **Repository Management**: Initialize, validate, and monitor repositories
- **Modset Operations**: Create, update, and manage modsets via command line
- **Shell Completion**: Tab completion for bash and zsh
- **Configuration**: Centralized config at `~/.a4sync/config.properties`

## 📚 Documentation

- **[Design Document](docs/design-document.md)** - Complete system architecture and technical design
- **[Technical Overview](docs/technical-overview.md)** - Current implementation status and capabilities
- **[Client Guide](docs/client-guide.md)** - Desktop application user guide
- **[Server Configuration](docs/server-configuration.md)** - Server setup and administration
- **[Repository Structure](docs/repository-structure.md)** - Repository layout and organization
- **[Docker Guide](docs/docker-guide.md)** - Containerized deployment
- **[CLI Reference](docs/cli-reference.md)** - Command line tools documentation
- **[Authentication Setup](docs/authentication-setup.md)** - Security configuration

## 🏗️ Architecture

A4Sync is built as a multi-module Maven project with clear separation of concerns:

- **a4sync-common**: Shared models and utilities (`ModSet`, `Mod`, `A4SyncConfig`)
- **a4sync-server**: Spring Boot REST API with file serving and Discord integration
- **a4sync-client**: JavaFX desktop application with multi-repository management
- **a4sync-tools**: Picocli CLI tools for repository administration

### Communication Flow
```
Client                 Server                 External
┌──────┐     HTTP     ┌──────┐              ┌─────────┐
│ GUI  │◄───────────►│ API  │─────────────►│ Discord │
│      │  Range      │      │   Webhooks   │         │
└──────┘  Requests   └──────┘              └─────────┘
    │                     │
    └─Steam Launch────────┴─File Storage
```

### Manual Installation

#### Requirements
- **Java 21** or later (OpenJDK recommended)
- **Maven 3.8+** for building from source
- **2GB RAM** minimum for server
- **Storage space** for mod files

#### Building from Source
```bash
# Build all modules
mvn clean package

# Run individual components
java -jar a4sync-server/target/a4sync-server-*.jar
java -jar a4sync-client/target/a4sync-client-*.jar  
java -jar a4sync-tools/target/a4sync-tools-*.jar --help
```

#### Configuration Files

**Server** (`application.properties`):
```properties
a4sync.root-directory=/a4sync
a4sync.authentication-enabled=false
a4sync.discord.enabled=false
server.port=8080
```
### Repository Setup
```bash
# Initialize repository structure
java -jar a4sync-tools.jar repo init /path/to/mods

# Create modset
java -jar a4sync-tools.jar modset create "Unit Alpha"

# Add mods to modset
java -jar a4sync-tools.jar mod add "@CBA_A3" --modset "Unit Alpha"
```

## 🔄 Workflow Example

### Server Administrator
```bash
# 1. Initialize repository
java -jar a4sync-tools.jar repo init /path/to/mods
java -jar a4sync-tools.jar config init

# 2. Create modsets
java -jar a4sync-tools.jar modset create "Training Mission Alpha"
java -jar a4sync-tools.jar mod add "@CBA_A3" --modset "Training Mission Alpha"
java -jar a4sync-tools.jar mod add "@ace" --modset "Training Mission Alpha"

# 3. Start server
java -jar a4sync-server.jar
```

### End User
1. **Launch Client**: Run `a4sync-client.jar` 
2. **Add Repository**: Enter server URL (auto-discovers configuration)
3. **Browse Modsets**: View available modsets with mod lists and sizes
4. **Download Mods**: Select modsets and download with resume capability
5. **Launch Game**: Click "Launch Arma" with mods automatically loaded

## 📋 API Endpoints

The A4Sync server provides a comprehensive REST API:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/modsets` | GET | List all available modsets |
| `/api/v1/modsets/{name}` | GET | Get specific modset details |
| `/api/v1/modsets/{modset}/mods/{mod}` | GET | Download mod files (Range support) |
| `/api/v1/repository/info` | GET | Repository metadata and statistics |
| `/api/v1/health` | GET | Health check endpoint |
| `/a4sync.json` | GET | A4Sync configuration (auto-discovery) |

Full API documentation available at `/swagger-ui/` when server is running.

## 🔧 Configuration Reference

### Server Properties
```properties
# Repository Configuration
a4sync.root-directory=/a4sync
a4sync.authentication-enabled=false
a4sync.repository-password=yourPassword

# Discord Integration (Optional)
a4sync.discord.enabled=false
a4sync.discord.webhook-url=https://discord.com/api/webhooks/...
a4sync.discord.username=A4Sync Server
a4sync.discord.embed-color=5814783

# Server Settings
server.port=8080
```

### Client Configuration
The client supports both portable and installed modes:
- **Portable**: `./a4sync-client-config.json` (working directory)
- **Installed**: `~/.a4sync/a4sync-client-config.json` (user home)

### CLI Configuration
```bash
# Create CLI config
java -jar a4sync-tools.jar config init

# Edit ~/.a4sync/config.properties
repository.default=/path/to/default/repo
network.timeout=30
logging.level=INFO
```

## 🚀 Performance & Reliability

### Download Performance
- **Chunked Architecture**: 1MB chunks for optimal resume capability
- **Parallel Downloads**: 4 simultaneous chunks (configurable)
- **Memory Efficiency**: ~10MB RAM per active download regardless of file size
- **Network Optimization**: HTTP/1.1 keep-alive and connection pooling

### Military-Grade Reliability
- **Integrity Verification**: SHA-256 checksums at file and chunk level
- **Error Recovery**: Exponential backoff retry logic
- **Failover Support**: Automatic mirror URL switching
- **Health Monitoring**: Continuous repository status checking
- **Graceful Degradation**: Continues operation when repositories unavailable

## 🔒 Security Features

- **File Integrity**: SHA-256 cryptographic verification prevents tampering
- **Secure Authentication**: BCrypt password hashing with configurable rounds
- **Path Validation**: Server prevents directory traversal attacks
- **HTTPS Support**: TLS encryption for all communications
- **Rate Limiting**: Protection against abuse and DoS attacks

## 🛠️ Development

### Building from Source
```bash
# Requirements: Java 21, Maven 3.8+
git clone https://github.com/Loafabreadly/a4sync.git
cd a4sync

# Build all modules
mvn clean package

# Run tests
mvn test

# Generate documentation
mvn site
```

### IDE Setup
- **IntelliJ IDEA**: Import Maven project, enable annotation processing
- **Eclipse**: Import as Maven project, install Lombok plugin
- **VS Code**: Java Extension Pack, Lombok Annotations Support

### Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details..