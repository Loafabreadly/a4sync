# A4Sync

A modern, efficient mod synchronization tool for Arma 4, inspired by Arma 3 Sync. It provides a complete solution for managing and distributing Arma 4 mods through an intuitive client, robust server, and powerful command-line tools.

## Features

- 📁 Efficient mod synchronization with partial downloads and resume support
- 🔄 Multiple mod set support with flexible organization
- 🔒 Repository authentication with SHA-256 password hashing
- 📊 File integrity verification with checksums
- 🛠️ CLI tools with tab auto-completion for repository management
- 🐳 Docker support for easy deployment

## Quick Links

- [Client Guide](docs/client-guide.md) - How to use the A4Sync client
- [Server Configuration](docs/server-configuration.md) - Setting up and managing the server
- [Docker Guide](docs/docker-guide.md) - Running A4Sync in Docker
- [CLI Reference](docs/cli-reference.md) - Command line tool documentation
- [Repository Structure](docs/repository-structure.md) - How to organize mods and mod sets

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