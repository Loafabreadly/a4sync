# Server Configuration Guide

This guide explains how to set up and configure the A4Sync server for hosting mod repositories.

## Quick Start

1. **Setup Repository** (use a4sync-tools CLI):
   ```bash
   # Initialize repository
   java -jar a4sync-tools.jar repo init /a4sync
   
   # Add mods
   java -jar a4sync-tools.jar mod add /a4sync @MyMod --source=/path/to/mod
   
   # Create mod sets
   java -jar a4sync-tools.jar modset create "Training Mods"
   ```

2. **Configure Server** (`application.properties`):
   ```properties
   # Repository root (must match CLI init path)
   a4sync.root-directory=/a4sync
   
   # Server port
   server.port=8080
   
   # Authentication (optional)
   a4sync.authentication-enabled=false
   #a4sync.repository-password=yourPassword
   ```

3. **Start Server**:
   ```bash
   java -jar a4sync-server.jar
   ```

## Server Configuration

The server requires minimal configuration. Repository management is handled by the CLI tools.

### Required Settings
```properties
# Repository root directory (initialized with CLI)
a4sync.root-directory=/a4sync

# Server port
server.port=8080
```

### Optional Settings
```properties
# Authentication
a4sync.authentication-enabled=false
a4sync.repository-password=yourSecretPassword

# Performance tuning (see application-example.properties)
server.compression.enabled=true
spring.servlet.multipart.max-file-size=5GB
```

## Repository Structure

Repositories are created and managed using the a4sync-tools CLI. The resulting structure is:
```
/a4sync/
  ├── @Mod1/                 # Mod directories (created by CLI)
  │   ├── mod.cpp
  │   ├── addons/*.pbo
  │   └── keys/*.bikey
  ├── @Mod2/
  │   └── ...
  └── modsets/               # Mod set definitions (created by CLI)
      ├── training.json
      └── operations.json
```

## Deployment Guide

### Phase 1: Repository Setup (CLI Tools)

Use a4sync-tools to initialize and populate your repository:

```bash
# 1. Download tools
wget https://github.com/your-org/a4sync/releases/latest/download/a4sync-tools.jar

# 2. Create configuration
java -jar a4sync-tools.jar config init
# Edit ~/.a4sync/config.properties to set repository.default=/a4sync

# 3. Initialize repository
java -jar a4sync-tools.jar repo init

# 4. Add your mods
java -jar a4sync-tools.jar mod add "@CBA_A4" --source=/path/to/cba
java -jar a4sync-tools.jar mod add "@ACE" --source=/path/to/ace

# 5. Create mod sets
java -jar a4sync-tools.jar modset create "Basic Mods"
java -jar a4sync-tools.jar modset add "Basic Mods" @CBA_A4 @ACE

# 6. Verify setup
java -jar a4sync-tools.jar repo status
```

### Phase 2: Server Configuration & Deployment

Configure and start the server to serve your repository:

```bash
# 1. Create server config
cat > application.properties << EOF
a4sync.root-directory=/a4sync
server.port=8080
a4sync.authentication-enabled=false
EOF

# 2. Start server
java -jar a4sync-server.jar
```

### Phase 3: Production Considerations

#### Memory Configuration

Adjust JVM memory settings based on your repository size:
```bash
# For repositories < 50GB
export JAVA_OPTS="-Xmx2G -Xms512M"

# For repositories > 50GB
export JAVA_OPTS="-Xmx4G -Xms1G"
```

### Behind a Reverse Proxy

When running behind Nginx or Apache, add these settings:
```properties
server.forward-headers-strategy=NATIVE
server.tomcat.remoteip.protocol-header=X-Forwarded-Proto
server.tomcat.remoteip.remote-ip-header=X-Forwarded-For
```

Example Nginx configuration:
```nginx
location / {
    proxy_pass http://localhost:8080;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Real-IP $remote_addr;
}
```

## Monitoring

The server includes Spring Boot Actuator endpoints for monitoring:

- Health check: `http://server:8080/actuator/health`
- Metrics: `http://server:8080/actuator/metrics`
- Repository stats: `http://server:8080/actuator/repository`

## Backup Strategy

1. Stop the server
2. Backup the `/mods` directory
3. Backup the configuration file
4. Restart the server

Use the following script for automated backups:
```bash
#!/bin/bash
DATE=$(date +%Y%m%d)
BACKUP_DIR="/backups/a4sync"

# Stop the server
docker compose down

# Create backup
tar czf "$BACKUP_DIR/mods-$DATE.tar.gz" mods/
cp config/application.properties "$BACKUP_DIR/config-$DATE.properties"

# Restart the server
docker compose up -d
```

## Troubleshooting

### Common Issues

1. **Server won't start**
   - Check logs: `docker compose logs server`
   - Verify file permissions
   - Ensure ports aren't in use

2. **Slow downloads**
   - Check `max-chunk-size` setting
   - Verify network bandwidth
   - Adjust `parallel-downloads`

3. **Authentication issues**
   - Verify bcrypt password hashes
   - Check client credentials
   - Confirm auth settings

### Log Analysis

Important log patterns to watch for:
```
ERROR c.a4sync.server.service.ModService - Failed to scan mod directory
WARN  c.a4sync.server.service.AuthService - Invalid login attempt
INFO  c.a4sync.server.service.SyncService - Started mod synchronization
```

## Performance Tuning

### JVM Options

For optimal performance:
```bash
JAVA_OPTS="\
    -Xmx2G \
    -Xms512M \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication"
```

### Network Settings

Adjust these based on your environment:
```properties
server.tomcat.max-threads=200
server.tomcat.max-connections=10000
server.tomcat.accept-count=100
```