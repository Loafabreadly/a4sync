# A4Sync

A robust, military-grade mod synchronization tool for Arma 4, featuring advanced chunked downloads, resume capability, and comprehensive integrity verification. Built for military units requiring reliable mod distribution.

## ✨ Features

### Phase 1: Core Sync Engine ✅
- 🚀 **Chunked Downloads**: Large files split into resumable chunks (50MB default)
- ⚡ **Resume Capability**: Automatically resume interrupted downloads
- 📊 **Real-time Progress**: Speed monitoring and ETA calculations
- 🔐 **Integrity Verification**: Multi-layer SHA-256 checksum validation
- ✅ **Mod Validation**: Comprehensive Arma mod structure verification

### Phase 2: Production Readiness ✅ 
- 🔧 **Custom Configuration**: A4Sync format (replaces .a3s standard)
- � **Auto-Discovery**: Intelligent repository configuration detection
- 📡 **Enhanced API**: Repository info and configuration endpoints
- 🏛️ **Military Standards**: Designed for operational reliability

### Core Features
- 🔄 Multiple mod set support with flexible organization
- 🔒 Repository authentication with BCrypt password hashing
- 🛠️ CLI tools with tab auto-completion for repository management
- 🐳 Docker support for easy deployment
- 📱 Modern JavaFX client interface

## 📚 Documentation

- [Client Guide](docs/client-guide.md) - How to use the A4Sync client with robust sync engine
- [Server Configuration](docs/server-configuration.md) - Setting up and managing the server
- [Repository Structure](docs/repository-structure.md) - Enhanced structure with A4Sync configuration
- [Docker Guide](docs/docker-guide.md) - Running A4Sync in Docker
- [CLI Reference](docs/cli-reference.md) - Command line tool documentation

## Getting Started

### Download

Download the latest version from [Releases](../../releases):
- `a4sync-client.jar` - Desktop client for downloading mods
- `a4sync-server.jar` - Server for hosting mod repositories
- `a4sync-tools.jar` - CLI tools for repository management

### Requirements

- Java 21 or later
- For server: 
  - 2GB RAM minimum
  - Storage space for mods
  - Optional: Docker for containerized deployment

### Quick Start Commands

```bash
# Start the client
java -jar a4sync-client.jar

# Start the server
java -jar a4sync-server.jar --spring.config.location=application.properties

# Use CLI tools (with tab completion)
java -jar a4sync-tools.jar repo status /path/to/repo

# Setup tab completion
./scripts/setup-completion.sh --jar-path a4sync-tools.jar
```

For detailed setup and usage instructions, please refer to the documentation links above.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

Please read our documentation before contributing.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.