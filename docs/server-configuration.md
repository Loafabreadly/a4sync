# Server Configuration Guide

This guide explains how to set up and configure the A4Sync server for hosting mod repositories.

## Configuration File

The server uses a `application.properties` file for configuration. Here's a detailed explanation of each setting:

```properties
# Server Settings
server.port=8080
server.compression.enabled=true
server.compression.mime-types=application/json,application/octet-stream

# Repository Settings
repository.name=My Arma 4 Repository
repository.description=Official repository for our unit's mods
repository.path=/mods
repository.max-chunk-size=52428800  # 50MB chunks
repository.parallel-downloads=3

# Security Settings (Optional)
repository.auth.enabled=false
repository.auth.type=basic
repository.auth.users[0].username=admin
repository.auth.users[0].password={bcrypt}$2a$10$...
```

## Directory Structure

The server expects the following directory structure:
```
/mods/
  ├── @Mod1/
  │   ├── mod.json
  │   ├── addons/
  │   └── keys/
  ├── @Mod2/
  │   └── ...
  └── modsets/
      ├── training.json
      └── operations.json
```

## Running in Production

### Memory Configuration

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