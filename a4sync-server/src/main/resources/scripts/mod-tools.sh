#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to calculate directory size
get_dir_size() {
    local dir="$1"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        du -sk "$dir" | cut -f1
    else
        # Linux
        du -sb "$dir" | cut -f1
    fi
}

# Function to calculate SHA-256 hash of a directory
get_dir_hash() {
    local dir="$1"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        find "$dir" -type f -exec shasum -a 256 {} \; | sort | shasum -a 256 | cut -d' ' -f1
    else
        # Linux
        find "$dir" -type f -exec sha256sum {} \; | sort | sha256sum | cut -d' ' -f1
    fi
}

# Function to create mod.json
create_mod_json() {
    local mod_dir="$1"
    local mod_name=$(basename "$mod_dir")
    local size=$(get_dir_size "$mod_dir")
    local hash=$(get_dir_hash "$mod_dir")
    
    # Extract version from directory name if it follows format @MOD_NAME-v1.2.3
    local version="1.0.0"
    if [[ $mod_name =~ -v([0-9]+\.[0-9]+\.[0-9]+) ]]; then
        version="${BASH_REMATCH[1]}"
    fi
    
    echo -e "${BLUE}Creating mod.json for $mod_name${NC}"
    cat > "$mod_dir/mod.json" << EOF
{
  "name": "$mod_name",
  "version": "$version",
  "size": $size,
  "hash": "$hash"
}
EOF
    echo -e "${GREEN}Created $mod_dir/mod.json${NC}"
}

# Function to create modset.json
create_modset_json() {
    local mods_dir="$1"
    local modset_name="$2"
    local profile_name="${3:-${modset_name}}"
    local description="${4:-${modset_name} mod set}"
    
    # Create modsets directory if it doesn't exist
    mkdir -p "$mods_dir/modsets"
    
    # Get list of mod directories (starting with @)
    local mod_list=""
    for mod in "$mods_dir"/@*/; do
        if [ -d "$mod" ]; then
            mod_name=$(basename "$mod")
            mod_list="$mod_list    \"$mod_name\",\n"
        fi
    done
    # Remove last comma and newline
    mod_list=$(echo -e "$mod_list" | sed '$ s/,$//')
    
    echo -e "${BLUE}Creating modset.json for $modset_name${NC}"
    cat > "$mods_dir/modsets/$modset_name.json" << EOF
{
  "name": "$modset_name",
  "description": "$description",
  "gameOptions": {
    "profileName": "$profile_name",
    "noSplash": true
  },
  "mods": [
$mod_list
  ]
}
EOF
    echo -e "${GREEN}Created $mods_dir/modsets/$modset_name.json${NC}"
}

# Function to update all mod.json files
update_all_mods() {
    local mods_dir="$1"
    for mod in "$mods_dir"/@*/; do
        if [ -d "$mod" ]; then
            create_mod_json "$mod"
        fi
    done
}

# Main script
case "$1" in
    "mod")
        if [ -z "$2" ]; then
            echo -e "${RED}Error: Mod directory not specified${NC}"
            echo "Usage: $0 mod <mod_directory>"
            exit 1
        fi
        create_mod_json "$2"
        ;;
    "modset")
        if [ -z "$2" ] || [ -z "$3" ]; then
            echo -e "${RED}Error: Missing arguments${NC}"
            echo "Usage: $0 modset <mods_directory> <modset_name> [profile_name] [description]"
            exit 1
        fi
        create_modset_json "$2" "$3" "${4:-$3}" "${5:-}"
        ;;
    "update-all")
        if [ -z "$2" ]; then
            echo -e "${RED}Error: Mods directory not specified${NC}"
            echo "Usage: $0 update-all <mods_directory>"
            exit 1
        fi
        update_all_mods "$2"
        ;;
    *)
        echo "A4Sync Mod Management Tool"
        echo
        echo "Usage:"
        echo "  $0 mod <mod_directory>                    - Create/update mod.json for a single mod"
        echo "  $0 modset <mods_dir> <name> [profile] [desc] - Create a modset.json file"
        echo "  $0 update-all <mods_directory>           - Update all mod.json files in directory"
        echo
        echo "Examples:"
        echo "  $0 mod \"/mods/@CUP_Terrains\""
        echo "  $0 modset \"/mods\" \"tactical\" \"tac_ops\" \"Tactical Operations Modset\""
        echo "  $0 update-all \"/mods\""
        exit 1
        ;;
esac