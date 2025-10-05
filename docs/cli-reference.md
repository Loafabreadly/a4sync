# A4Sync CLI Reference

The A4Sync CLI tool (`a4sync-tools.jar`) provides commands for managing mod repositories, mod sets, and configurations.

## Installation & Setup

### Download
```bash
# Download from GitHub releases
wget https://github.com/your-org/a4sync/releases/latest/download/a4sync-tools.jar

# Or build from source
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

# Edit configuration (opens default editor)  
java -jar a4sync-tools.jar config edit

# View current configuration
java -jar a4sync-tools.jar config show
```

## Global Options

All commands support these global options:

- `-h, --help` - Show help for the command
- `-v, --verbose` - Enable verbose output
- `-q, --quiet` - Suppress non-error output

## Repository Commands

### `repo init`
Initialize a new mod repository.

```bash
java -jar a4sync-tools.jar repo init [options] [path]
```

**Options:**
- `--repository, -r <path>` - Repository path (default: from config)
- `--force` - Overwrite existing repository

**Examples:**
```bash
# Initialize with default path from config
java -jar a4sync-tools.jar repo init

# Initialize specific path
java -jar a4sync-tools.jar repo init /srv/a4sync

# Force overwrite existing
java -jar a4sync-tools.jar repo init --force
```

### `repo validate`
Validate repository structure and integrity.

```bash
java -jar a4sync-tools.jar repo validate [options]
```

**Options:**
- `--repository, -r <path>` - Repository path (default: from config)
- `--fix` - Automatically fix issues when possible

### `repo status`
Show repository status and statistics.

```bash
java -jar a4sync-tools.jar repo status [options]
```

**Options:**
- `--repository, -r <path>` - Repository path (default: from config)

## Mod Commands

### `mod create`
Create a new mod in the repository.

```bash
java -jar a4sync-tools.jar mod create [options] <mod-directory>
```

**Arguments:**
- `<mod-directory>` - Path to the mod directory to create (must start with @)

**Options:**
- `--source <path>` - Copy files from source directory

**Examples:**
```bash
# Create empty mod (requires full path)
java -jar a4sync-tools.jar mod create /srv/a4sync/@MyMod

# Create from source
java -jar a4sync-tools.jar mod create /srv/a4sync/@MyMod --source=/path/to/mod
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
Update an existing mod from source.

```bash
java -jar a4sync-tools.jar mod update [options] <mod-directory>
```

**Arguments:**
- `<mod-directory>` - Path to the mod directory to update

**Options:**
- `--source <path>` - New source directory (required)

**Examples:**
```bash
# Update mod from new source
java -jar a4sync-tools.jar mod update /srv/a4sync/@MyMod --source=/path/to/updated/mod
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
- `--repository, -r <path>` - Repository path (default: from config)
- `--description <text>` - Description of the mod set

**Examples:**
```bash
# Create basic mod set
java -jar a4sync-tools.jar modset create "Training Mods"

# Create with description and specific repository
java -jar a4sync-tools.jar modset create "Training Mods" -r /srv/a4sync --description="Essential mods for training sessions"
```

### `modset list`
List all mod sets.

```bash
java -jar a4sync-tools.jar modset list [options]
```

**Options:**
- `--repository, -r <path>` - Repository path (default: from config)

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
- `--repository, -r <path>` - Repository path (default: from config)

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
- `--repository, -r <path>` - Repository path (default: from config)

**Examples:**
```bash
# Remove single mod
java -jar a4sync-tools.jar modset remove "Training Mods" @OldMod

# Remove multiple mods  
java -jar a4sync-tools.jar modset remove "Training Mods" @OldMod1 @OldMod2
```

## Configuration Commands

### `config init`
Initialize configuration with default values.

```bash
java -jar a4sync-tools.jar config init [options]
```

**Options:**
- `--force` - Overwrite existing configuration

### `config edit`
Open configuration file in default editor.

```bash
java -jar a4sync-tools.jar config edit
```

### `config show`
Display current configuration.

```bash
java -jar a4sync-tools.jar config show
```

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
# Default repository path
repository.default=/srv/a4sync

# Server settings (for future client operations)
server.default.url=http://localhost:8080
server.default.username=admin
server.default.password=password
```

## Examples

### Setting Up a New Repository

```bash
# 1. Initialize configuration
java -jar a4sync-tools.jar config init

# 2. Initialize repository
java -jar a4sync-tools.jar repo init /srv/a4sync

# 3. Add mods
java -jar a4sync-tools.jar mod create /srv/a4sync/@CBA_A4 --source=/steamapps/common/Arma4/Mods/@CBA_A4
java -jar a4sync-tools.jar mod create /srv/a4sync/@ACE --source=/steamapps/common/Arma4/Mods/@ACE

# 4. Create mod set
java -jar a4sync-tools.jar modset create "Basic Setup" -r /srv/a4sync
java -jar a4sync-tools.jar modset add "Basic Setup" @CBA_A4 @ACE -r /srv/a4sync

# 5. Verify
java -jar a4sync-tools.jar repo status -r /srv/a4sync

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
- `2` - Invalid arguments
- `3` - File/directory not found
- `4` - Permission denied
- `5` - Network error (for client operations)
a4sync modset add "Operations" @CBA_A4 @ACE @Operation_Mods

# List all sets
a4sync modset list

# Or specify repository explicitly
a4sync modset list -r /different/path
```