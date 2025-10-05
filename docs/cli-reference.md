# CLI Tool Reference

The A4Sync CLI tool provides comprehensive commands for managing mod repositories.

## Installation

Download the latest `a4sync-tools.jar` from [Releases](../../releases).

Create an alias for convenience:
```bash
alias a4sync='java -jar /path/to/a4sync-tools.jar'
```

## Repository Management

### Initialize Repository
```bash
a4sync repo init /path/to/repository [--auth]
```
Creates a new repository with the required structure:
- `/mods` directory for mod storage
- `/modsets` directory for mod set definitions
- `repository.properties` configuration file

### Validate Repository
```bash
a4sync repo validate /path/to/repository
```
Checks repository structure and files:
- Directory structure
- Mod configurations
- File permissions

### Show Repository Status
```bash
a4sync repo status /path/to/repository
```
Displays:
- Number of mods
- Number of mod sets
- Total repository size
- Last update time

## Mod Management

### Add Mod
```bash
a4sync mod add /path/to/repository "@ModName" [--source=/path/to/mod]
```
Adds a mod to the repository:
1. Copies mod files
2. Generates checksums
3. Creates mod.json metadata

### Update Mod
```bash
a4sync mod update /path/to/repository "@ModName" [--source=/path/to/mod]
```
Updates an existing mod:
1. Verifies changes
2. Updates checksums
3. Updates mod.json

### Remove Mod
```bash
a4sync mod remove /path/to/repository "@ModName"
```
Removes a mod and updates dependent mod sets.

### List Mods
```bash
a4sync mod list /path/to/repository [--details]
```
Shows installed mods with optional details:
- Size
- Version
- Last update
- Used in mod sets

## Mod Set Management

### Create Mod Set
```bash
a4sync modset create /path/to/repository "Set Name" [@Mod1 @Mod2...]
```
Creates a new mod set with specified mods.

### Update Mod Set
```bash
a4sync modset update /path/to/repository "Set Name" [@Mod1 @Mod2...]
```
Updates mod list in existing mod set.

### Remove Mod Set
```bash
a4sync modset remove /path/to/repository "Set Name"
```
Removes a mod set definition.

### List Mod Sets
```bash
a4sync modset list /path/to/repository [--details]
```
Shows defined mod sets with optional details:
- Included mods
- Total size
- Required space

## Advanced Features

### Verify Integrity
```bash
a4sync repo verify /path/to/repository
```
Verifies all mod checksums and fixes issues.

### Export/Import
```bash
# Export mod set
a4sync modset export /path/to/repository "Set Name" > modset.json

# Import mod set
a4sync modset import /path/to/repository modset.json
```

### Bulk Operations
```bash
# Update all mods
a4sync mod update-all /path/to/repository

# Verify all mods
a4sync mod verify-all /path/to/repository
```

## Configuration

### Global Settings
Create `~/.a4sync/config.properties`:
```properties
# Default repository path
repository.default=/path/to/repository

# Network settings
network.timeout=30
network.retries=3

# Logging
logging.level=INFO
logging.file=~/.a4sync/a4sync.log
```

### Repository-specific Settings
Edit `repository.properties` in repository root:
```properties
repository.name=My Repository
repository.description=Unit mods
repository.auth.enabled=false
repository.max-chunk-size=52428800
```

## Examples

### Setting Up a New Repository
```bash
# Initialize repository
a4sync repo init /mods

# Add mods
a4sync mod add /mods "@CBA_A4"
a4sync mod add /mods "@ACE"

# Create mod set
a4sync modset create /mods "Basic Mods" @CBA_A4 @ACE

# Verify setup
a4sync repo validate /mods
a4sync repo status /mods
```

### Updating Mods
```bash
# Update specific mod
a4sync mod update /mods "@ACE" --source=/downloads/@ACE

# Update all mods
a4sync mod update-all /mods

# Verify integrity
a4sync mod verify-all /mods
```

### Managing Mod Sets
```bash
# Create training set
a4sync modset create /mods "Training" @CBA_A4 @ACE @Training_Mods

# Create operations set
a4sync modset create /mods "Operations" @CBA_A4 @ACE @Operation_Mods

# List all sets
a4sync modset list /mods --details
```