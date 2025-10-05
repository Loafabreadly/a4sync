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

## API Endpoints

- `GET /api/v1/modsets` - List all mod sets
- `GET /api/v1/modsets/{name}` - Get mod set details
- `GET /api/v1/modsets/{modSetName}/mods/{modName}` - Download a mod file
- `GET /api/v1/autoconfig` - Get automatic configuration

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
