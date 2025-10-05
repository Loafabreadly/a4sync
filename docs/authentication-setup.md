# A4Sync Authentication Setup Guide

## Overview

A4Sync supports secure BCrypt-based authentication for repository access with automatic HTTPS preference and HTTP fallback for local development.

## Security Features

- **BCrypt password hashing** - Passwords are never stored in plain text
- **HTTPS-first approach** - Automatically tries HTTPS before falling back to HTTP  
- **Plain text password transmission** - Users enter passwords in plain text in the UI (encrypted over HTTPS)
- **Flexible deployment** - Works in both production (HTTPS) and local development (HTTP) environments

## Server Setup

### 1. Enable Authentication

Add these properties to your `application.properties`:

```properties
# Enable authentication
a4sync.authentication-enabled=true

# Option 1: Use plain text password (server will hash automatically)
a4sync.repository-password=mySecretPassword

# Option 2: Use pre-generated BCrypt hash (more secure for production)
# a4sync.repository-password-hash=$2a$10$example.bcrypt.hash.here
```

### 2. Generate BCrypt Hash (Optional)

For production deployments, you can pre-generate the BCrypt hash:

```bash
# Run the password utility
java -cp a4sync-server.jar com.a4sync.server.util.PasswordUtils "mySecretPassword"
```

This will output:
```
BCrypt hash generated for password: mySecretPassword

Add this to your application.properties:
a4sync.authentication-enabled=true
a4sync.repository-password-hash=$2a$10$N9qo8uLOickgx2ZMRZoMye.IrBkR.9F3J7ku.1kGmZ8QxF7A4hW/K

Or alternatively, use the plain text password (less secure):
a4sync.authentication-enabled=true
a4sync.repository-password=mySecretPassword
```

### 3. HTTPS Configuration (Production)

For production deployments, configure HTTPS in `application.properties`:

```properties
# HTTPS Configuration (Production)
server.port=443
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=a4sync
```

## Client Setup

### 1. Repository Configuration

1. Open the A4Sync client
2. Go to the "Repository" tab or use "Manage Repositories"
3. Click "Add Repository"
4. Fill in the form:
   - **Name**: A friendly name for the repository
   - **URL**: The server URL (can be HTTP or HTTPS)
   - **Password**: Enter the plain text password
   - **Enabled**: Check to enable this repository
   - **Check on startup**: Check to automatically check for updates

### 2. Connection Testing

The client will automatically:
1. Try HTTPS first if no protocol is specified
2. Fall back to HTTP if HTTPS fails
3. Show connection status and any authentication errors

## Authentication Flow

1. **User enters plain text password** in the client UI
2. **Client sends password over HTTPS** (or HTTP for local testing)
3. **Server verifies password** against stored BCrypt hash
4. **Access granted/denied** based on verification result

## Security Considerations

### Production Deployment
- Always use HTTPS in production
- Use `a4sync.repository-password-hash` with a pre-generated BCrypt hash
- Never store plain text passwords in configuration files
- Use strong passwords (12+ characters with mixed case, numbers, symbols)

### Local Development
- HTTP is acceptable for local testing
- Plain text password configuration is acceptable for development
- Use different passwords for development vs production

## Troubleshooting

### Common Issues

1. **Authentication Failed**: 
   - Verify the password is correct
   - Check server logs for authentication errors
   - Ensure `a4sync.authentication-enabled=true`

2. **Connection Failed**:
   - Check if HTTPS certificate is valid (production)
   - Try HTTP URL for local testing  
   - Verify server is running and accessible

3. **SSL/TLS Issues**:
   - Check certificate configuration
   - Verify certificate is not expired
   - Try HTTP fallback for troubleshooting

### Server Logs

Enable debug logging to troubleshoot authentication:

```properties
logging.level.com.a4sync.server.security=DEBUG
```

## Migration from SHA-256

If migrating from the previous SHA-256 authentication:

1. Update server configuration to use BCrypt
2. Restart server
3. Client configuration remains the same (users still enter plain text passwords)
4. Test connection and authentication

The migration is seamless for end users - they continue entering plain text passwords in the UI.