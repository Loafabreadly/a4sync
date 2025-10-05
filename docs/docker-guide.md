# Docker Guide

This guide explains how to use Docker to run the A4Sync server.

## Prerequisites

1. Install Docker:
   ```bash
   # Ubuntu/Debian
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   
   # Or follow instructions for your OS:
   # https://docs.docker.com/engine/install/
   ```

2. Install Docker Compose:
   ```bash
   # Ubuntu/Debian
   sudo apt-get update
   sudo apt-get install docker-compose-plugin
   ```

## Basic Setup

1. Create a directory for your server:
   ```bash
   mkdir a4sync-server
   cd a4sync-server
   ```

2. Create required directories:
   ```bash
   mkdir mods config
   ```

3. Create `docker-compose.yml`:
   ```yaml
   version: '3.8'
   
   services:
     server:
       image: ghcr.io/loafabreadly/a4sync-server:latest
       ports:
         - "8080:8080"
       volumes:
         - ./mods:/mods
         - ./config:/config
       environment:
         - JAVA_OPTS=-Xmx2G -Xms512M
       restart: unless-stopped
       healthcheck:
         test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/actuator/health"]
         interval: 30s
         timeout: 10s
         retries: 3
   ```

4. Create server configuration:
   ```bash
   cat > config/application.properties << EOF
   # Repository root (will be mounted as volume)
   a4sync.root-directory=/data
   
   # Server configuration
   server.port=8080
   
   # Authentication (optional)
   a4sync.authentication-enabled=false
   #a4sync.repository-password=yourPassword
   EOF
   ```

5. Setup repository using CLI tools:
   ```bash
   # Download CLI tools
   wget https://github.com/Loafabreadly/a4sync/releases/latest/download/a4sync-tools.jar
   
   # Initialize repository in data directory
   java -jar a4sync-tools.jar repo init ./data
   
   # Add your mods
   java -jar a4sync-tools.jar mod add ./data "@YourMod" --source=/path/to/mod
   
   # Create mod sets
   java -jar a4sync-tools.jar modset create -r ./data "Default Set"
   java -jar a4sync-tools.jar modset add -r ./data "Default Set" @YourMod
   ```

6. Start the server:
   ```bash
   docker compose up -d
   ```

## Advanced Configuration

### Memory Settings

Adjust `JAVA_OPTS` based on your needs:
```yaml
services:
  server:
    environment:
      - JAVA_OPTS=-Xmx4G -Xms1G -XX:+UseG1GC
```

### Using a Different Port

Change the port mapping:
```yaml
services:
  server:
    ports:
      - "9090:8080"  # Server accessible on port 9090
```

### Enabling HTTPS

1. Create certificates:
   ```bash
   mkdir certs
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout certs/server.key -out certs/server.crt
   ```

2. Update `docker-compose.yml`:
   ```yaml
   services:
     server:
       volumes:
         - ./mods:/mods
         - ./config:/config
         - ./certs:/certs
       environment:
         - SERVER_SSL_ENABLED=true
         - SERVER_SSL_KEY_STORE=/certs/keystore.p12
         - SERVER_SSL_KEY_STORE_PASSWORD=changeit
   ```

### Using a Reverse Proxy

Example with Nginx:

1. Create `nginx.conf`:
   ```nginx
   server {
       listen 80;
       server_name mods.yourdomain.com;
   
       location / {
           proxy_pass http://server:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }
   ```

2. Update `docker-compose.yml`:
   ```yaml
   version: '3.8'
   
   services:
     server:
       image: ghcr.io/loafabreadly/a4sync-server:latest
       expose:
         - "8080"
       volumes:
         - ./mods:/mods
         - ./config:/config
   
     nginx:
       image: nginx:alpine
       ports:
         - "80:80"
       volumes:
         - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
       depends_on:
         - server
   ```

## Maintenance

### Updating the Server

1. Pull latest image:
   ```bash
   docker compose pull
   ```

2. Restart containers:
   ```bash
   docker compose down
   docker compose up -d
   ```

### Backup and Restore

1. Create backup script:
   ```bash
   #!/bin/bash
   
   # Stop server
   docker compose down
   
   # Backup
   tar czf backup-$(date +%Y%m%d).tar.gz mods/ config/
   
   # Restart server
   docker compose up -d
   ```

2. Restore from backup:
   ```bash
   docker compose down
   tar xzf backup-20251004.tar.gz
   docker compose up -d
   ```

### Monitoring

1. View logs:
   ```bash
   # All logs
   docker compose logs
   
   # Follow new logs
   docker compose logs -f
   
   # Last 100 lines
   docker compose logs --tail=100
   ```

2. Check container status:
   ```bash
   docker compose ps
   ```

3. View resource usage:
   ```bash
   docker stats
   ```

## Troubleshooting

### Container Won't Start

1. Check logs:
   ```bash
   docker compose logs server
   ```

2. Verify file permissions:
   ```bash
   sudo chown -R 1000:1000 mods config
   ```

3. Test configuration:
   ```bash
   docker compose config
   ```

### Performance Issues

1. Check resource usage:
   ```bash
   docker stats
   ```

2. Adjust memory limits:
   ```yaml
   services:
     server:
       deploy:
         resources:
           limits:
             memory: 4G
           reservations:
             memory: 1G
   ```

3. Monitor disk I/O:
   ```bash
   docker stats --format "table {{.Name}}\t{{.BlockIO}}"
   ```

### Network Issues

1. Verify port binding:
   ```bash
   docker compose ps
   netstat -tulpn | grep 8080
   ```

2. Check container networking:
   ```bash
   docker network ls
   docker network inspect a4sync-server_default
   ```

3. Test connectivity:
   ```bash
   curl -v http://localhost:8080/actuator/health
   ```