# A4Sync CLI Reference

The A4Sync CLI tool (`a4sync-tools.jar`) provides commands for managing mod repositories, mod sets, and configurations.

## Installation & Setup

### Download
```bash
# Build from source
mvn clean package -f a4sync-tools/pom.xml
```

### Tab Auto-completion

Enable tab completion for improved usability:

```bash
# Generate and install completion script automatically
./scripts/setup-completion.sh --jar-path ./a4sync-tools.jar

# Or manually generate for bash
java -jar a4sync-tools.jar completion bash > ~/.bash_completion.d/a4sync
source ~/.bash_completion.d/a4sync

# Or manually generate for zsh
java -jar a4sync-tools.jar completion zsh > ~/.zsh/completions/_a4sync
# Add to ~/.zshrc: fpath=(~/.zsh/completions $fpath)
```

### Initial Configuration
```bash
# Initialize configuration file
java -jar a4sync-tools.jar config init
```

## Global Options

All commands support:

- `-h, --help` - Show help for the command

## Repository Commands

### `repo init`
Initialize a new mod repository.

```bash
java -jar a4sync-tools.jar repo init [path]
```

**Arguments:**
- `[path]` - Repository path (optional if configured in ~/.a4sync/config.properties)

**Options:**
- `--auth` - Enable authentication

**Examples:**
```bash
# Initialize with default path from config
java -jar a4sync-tools.jar repo init

# Initialize specific path
java -jar a4sync-tools.jar repo init /srv/a4sync

# Initialize with authentication enabled
java -jar a4sync-tools.jar repo init /srv/a4sync --auth
```

### `repo validate`
Validate repository structure and integrity.

```bash
java -jar a4sync-tools.jar repo validate [path]
```

**Arguments:**
- `[path]` - Repository path (optional if configured in ~/.a4sync/config.properties)

### `repo status`
Show repository status and statistics.

```bash
java -jar a4sync-tools.jar repo status [path]
```

**Arguments:**
- `[path]` - Repository path (optional if configured in ~/.a4sync/config.properties)

## Mod Commands

### `mod create`
Create a new mod configuration.

```bash
java -jar a4sync-tools.jar mod create [options] <mod-directory>
```

**Arguments:**
- `<mod-directory>` - Path to the mod directory

**Options:**
- `-v, --version <version>` - Mod version (default: 1.0.0)

**Examples:**
```bash
# Create mod with default version
java -jar a4sync-tools.jar mod create /srv/a4sync/@MyMod

# Create mod with specific version
java -jar a4sync-tools.jar mod create /srv/a4sync/@MyMod --version 2.1.0
```

### `mod list`
List all mods in the repository.

```bash
java -jar a4sync-tools.jar mod list [options] <mod-directory>
```

**Arguments:**
- `<mod-directory>` - Repository directory path

**Examples:**
```bash
# List mods in repository
java -jar a4sync-tools.jar mod list /srv/a4sync
```

### `mod update`
Update an existing mod configuration.

```bash
java -jar a4sync-tools.jar mod update <mod-directory>
```

**Arguments:**
- `<mod-directory>` - Path to the mod directory to update

**Examples:**
```bash
# Update mod configuration (recalculates checksums, etc.)
java -jar a4sync-tools.jar mod update /srv/a4sync/@MyMod
```

## Mod Set Commands

### `modset create`
Create a new mod set.

```bash
java -jar a4sync-tools.jar modset create [options] <name>
```

**Arguments:**
- `<name>` - Name of the mod set

**Options:**
- `-r, --repository <path>` - Repository path (optional if configured in ~/.a4sync/config.properties)
- `-d, --description <text>` - Description of the mod set

**Examples:**
```bash
# Create basic mod set
java -jar a4sync-tools.jar modset create "Training Mods"

# Create with description and specific repository
java -jar a4sync-tools.jar modset create "Training Mods" -r /srv/a4sync --description "Essential mods for training sessions"
```

### `modset list`
List all mod sets.

```bash
java -jar a4sync-tools.jar modset list [options]
```

**Options:**
- `-r, --repository <path>` - Repository path (optional if configured in ~/.a4sync/config.properties)

**Examples:**
```bash
# List mod sets using config default
java -jar a4sync-tools.jar modset list

# List mod sets in specific repository  
java -jar a4sync-tools.jar modset list -r /srv/a4sync
```

### `modset add`
Add mods to a mod set.

```bash
java -jar a4sync-tools.jar modset add [options] <modset-name> <mod-name>...
```

**Arguments:**
- `<modset-name>` - Name of the mod set
- `<mod-name>...` - Names of mods to add (space-separated)

**Options:**
- `-r, --repository <path>` - Repository path (optional if configured in ~/.a4sync/config.properties)

**Examples:**
```bash
# Add single mod
java -jar a4sync-tools.jar modset add "Training Mods" @CBA_A4

# Add multiple mods
java -jar a4sync-tools.jar modset add "Training Mods" @CBA_A4 @ACE @TFAR

# Use specific repository
java -jar a4sync-tools.jar modset add -r /srv/a4sync "Training Mods" @CBA_A4
```

### `modset remove`
Remove mods from a mod set.

```bash
java -jar a4sync-tools.jar modset remove [options] <modset-name> <mod-name>...
```

**Arguments:**
- `<modset-name>` - Name of the mod set
- `<mod-name>...` - Names of mods to remove (space-separated)

**Options:**
- `-r, --repository <path>` - Repository path (optional if configured in ~/.a4sync/config.properties)

**Examples:**
```bash
# Remove single mod
java -jar a4sync-tools.jar modset remove "Training Mods" @OldMod

# Remove multiple mods  
java -jar a4sync-tools.jar modset remove "Training Mods" @OldMod1 @OldMod2
```

## Configuration Commands

### `config init`
Initialize configuration file with default values.

```bash
java -jar a4sync-tools.jar config init
```

Creates `~/.a4sync/config.properties` with example configuration settings.

## Completion Commands

### `completion bash`
Generate bash completion script.

```bash
java -jar a4sync-tools.jar completion bash
```

### `completion zsh`  
Generate zsh completion script.

```bash
java -jar a4sync-tools.jar completion zsh
```

## Configuration File

The CLI uses `~/.a4sync/config.properties` for default settings:

```properties
# A4Sync Tools Configuration

# Default repository path (used when no path is specified)
repository.default=/path/to/repository

# Network settings
network.timeout=30
network.retries=3

# Logging
logging.level=INFO
logging.file=~/.a4sync/a4sync.log
```

## Examples

### Setting Up a New Repository

```bash
# 1. Initialize configuration
java -jar a4sync-tools.jar config init

# 2. Initialize repository
java -jar a4sync-tools.jar repo init /srv/a4sync

# 3. Create mod configurations (after copying mod files to repository)
java -jar a4sync-tools.jar mod create /srv/a4sync/@CBA_A4
java -jar a4sync-tools.jar mod create /srv/a4sync/@ACE

# 4. Create mod set
java -jar a4sync-tools.jar modset create "Basic Setup" -r /srv/a4sync
java -jar a4sync-tools.jar modset add "Basic Setup" @CBA_A4 @ACE -r /srv/a4sync

# 5. Verify
java -jar a4sync-tools.jar repo status /srv/a4sync
```

### Managing Mod Sets

```bash
# Create different environments
java -jar a4sync-tools.jar modset create "Training" -r /srv/a4sync
java -jar a4sync-tools.jar modset create "Operations" -r /srv/a4sync

# Add core mods to both
java -jar a4sync-tools.jar modset add "Training" @CBA_A4 @ACE -r /srv/a4sync
java -jar a4sync-tools.jar modset add "Operations" @CBA_A4 @ACE @TFAR -r /srv/a4sync

# Add specialized mods
java -jar a4sync-tools.jar modset add "Training" @VR_Training -r /srv/a4sync
java -jar a4sync-tools.jar modset add "Operations" @RHS_USAF @RHS_AFRF -r /srv/a4sync

# List all mod sets
java -jar a4sync-tools.jar modset list -r /srv/a4sync
```

### Using Tab Completion

After setting up completion:

```bash
# Tab through main commands
java -jar a4sync-tools.jar <TAB>
# Shows: completion config mod modset repo

# Tab through subcommands  
java -jar a4sync-tools.jar repo <TAB>
# Shows: init status validate

# Tab through modset commands
java -jar a4sync-tools.jar modset <TAB>
# Shows: add create list remove
```

## Exit Codes

- `0` - Success
- `1` - General error