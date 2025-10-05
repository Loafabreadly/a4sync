# Client Guide

This guide explains how to use the A4Sync client to download and manage Arma 4 mods with the robust sync engine.

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
   - Maximum concurrent downloads (recommended: 3)
   - Chunk size for large files (default: 50MB)

## Adding Repositories

### Manual Repository Addition
1. Click "Add Repository"
2. Enter repository details:
   - Name: A friendly name for the repository
   - URL: The repository address (e.g., `https://mods.unit.com:8080`)
   - Password (if required): Automatically hashed before transmission

### Auto-Discovery (New in Phase 2)
A4Sync can automatically discover repository configurations:

1. **Direct Config URL**: Enter a repository URL ending with `/a4sync.json`
2. **Auto-Discovery**: A4Sync will try common configuration endpoints:
   - `/.a4sync`
   - `/a4sync.json` 
   - `/config`
   - `/repository/info`

3. **Local Config Files**: Import `.a4sync` configuration files directly

## Managing Mods

### Downloading Mods (Enhanced Sync Engine)

1. Select a repository
2. Choose a mod set
3. Click "Download Selected"
4. Monitor real-time progress with:
   - **Download Speed**: Current transfer rate
   - **Progress**: Percentage and ETA
   - **Resume Capability**: Automatically resumes interrupted downloads
   - **Integrity Checking**: SHA-256 verification for all files

**Phase 1 Features:**
- **Chunked Downloads**: Large files split into manageable chunks
- **Resume Support**: Continue downloads from interruption point  
- **Progress Tracking**: Real-time speed and ETA calculations
- **Integrity Verification**: Automatic SHA-256 checksum validation

### Verifying Mods

1. Select installed mods
2. Click "Verify" - uses comprehensive validation:
   - **File Structure**: Validates Arma mod folder structure
   - **PBO Files**: Verifies addon files exist and are valid
   - **Signatures**: Checks BIKEY files and signatures
   - **Dependencies**: Validates mod dependencies are met
3. Review detailed results
4. Re-download any corrupted files automatically

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

### Robust Download System (Phase 1)

**Resume Capability:**
- Downloads automatically resume from interruption point
- No need to restart large downloads
- Intelligent partial file detection and validation

**Progress Tracking:**
- Real-time download speed monitoring
- Accurate ETA calculations
- Visual progress indicators with detailed statistics

**Integrity Assurance:**
- SHA-256 checksum verification for all downloads
- Automatic re-download of corrupted files
- Multi-layer validation (download + mod structure)

### Production Configuration System (Phase 2)

**A4Sync Configuration Format:**
- Custom `.a4sync` configuration format (replaces .a3s)
- Enhanced repository metadata and settings
- Auto-discovery from multiple endpoints

**Repository Auto-Discovery:**
- Intelligent configuration detection
- Fallback to multiple common endpoints
- Support for both HTTP and HTTPS with automatic preference

### Bandwidth Management

Control download speeds and resources:
1. Open Settings
2. Set maximum download speed
3. Adjust concurrent downloads (recommended: 3)
4. Configure chunk sizes for optimal performance

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