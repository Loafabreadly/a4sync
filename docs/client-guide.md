# Client Guide

This guide explains how to use the A4Sync client to download and manage Arma 4 mods.

## Installation

1. Download the latest client from [Releases](../../releases)
2. Ensure you have Java 21 or later installed:
   ```bash
   java -version
   ```
   If not, download from [Adoptium](https://adoptium.net/)

3. Run the client:
   ```bash
   java -jar a4sync-client.jar
   ```

## First Time Setup

1. Open A4Sync
2. Click "Settings" and configure:
   - Arma 4 installation path
   - Download directory
   - Maximum concurrent downloads

## Adding Repositories

1. Click "Add Repository"
2. Enter repository details:
   - Name: A friendly name for the repository
   - URL: The repository address (e.g., `http://mods.unit.com:8080`)
   - Authentication (if required):
     - Username
     - Password

## Managing Mods

### Downloading Mods

1. Select a repository
2. Choose a mod set
3. Click "Download Selected"
4. Monitor progress in the Downloads tab

### Verifying Mods

1. Select installed mods
2. Click "Verify"
3. Review results
4. Re-download any corrupted files

### Updating Mods

1. Click "Check for Updates"
2. Review available updates
3. Select mods to update
4. Click "Update Selected"

## Mod Sets

### Using Mod Sets

1. Select a repository
2. Choose a mod set
3. Click "Download Set"
4. Launch Arma 4 with the mod set:
   - Click "Launch"
   - Or copy the mod line for manual launch

### Creating Local Mod Sets

1. Click "New Mod Set"
2. Enter a name
3. Select mods from repositories
4. Click "Save"

### Exporting/Importing Mod Sets

1. Export:
   - Select a mod set
   - Click "Export"
   - Save the `.json` file

2. Import:
   - Click "Import"
   - Select a `.json` file
   - Review and confirm

## Advanced Features

### Partial Downloads

A4Sync supports resuming interrupted downloads:
1. Downloads are automatically paused if connection is lost
2. Click "Resume" to continue from where you left off

### Bandwidth Management

Control download speeds:
1. Open Settings
2. Set maximum download speed
3. Adjust concurrent downloads

### Custom Launch Options

Configure Arma 4 launch options:
1. Open Settings
2. Click "Launch Options"
3. Add custom parameters

## Troubleshooting

### Common Issues

1. **Download Fails**
   - Check internet connection
   - Verify repository URL
   - Ensure sufficient disk space

2. **Authentication Error**
   - Verify credentials
   - Check repository URL
   - Confirm repository requires auth

3. **Missing Mods**
   - Verify Arma 4 path
   - Check mod installation path
   - Run file verification

### Log Files

Find logs at:
- Windows: `%APPDATA%\A4Sync\logs`
- Linux: `~/.config/a4sync/logs`
- macOS: `~/Library/Application Support/A4Sync/logs`

### Getting Help

1. Check documentation
2. Review [common issues](../../wiki/Common-Issues)
3. [Create an issue](../../issues/new)