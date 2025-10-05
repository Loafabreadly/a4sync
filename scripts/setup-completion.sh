#!/bin/bash
# A4Sync CLI Auto-completion Setup Script

set -e

JAR_PATH=""
SHELL_TYPE=""
INSTALL_LOCATION=""

print_usage() {
    echo "Usage: $0 --jar-path /path/to/a4sync-tools.jar [--shell bash|zsh] [--install-location /path/to/install]"
    echo ""
    echo "Options:"
    echo "  --jar-path PATH        Path to a4sync-tools.jar (required)"
    echo "  --shell SHELL         Shell type: bash or zsh (auto-detected if not specified)" 
    echo "  --install-location    Custom installation path (optional)"
    echo "  --help               Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --jar-path ./a4sync-tools.jar"
    echo "  $0 --jar-path ./a4sync-tools.jar --shell bash"
    echo "  $0 --jar-path ./a4sync-tools.jar --shell zsh --install-location ~/.local/share/zsh/completions"
}

detect_shell() {
    if [[ -n "$ZSH_VERSION" ]]; then
        echo "zsh"
    elif [[ -n "$BASH_VERSION" ]]; then
        echo "bash"
    else
        echo "bash"  # default fallback
    fi
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --jar-path)
            JAR_PATH="$2"
            shift 2
            ;;
        --shell)
            SHELL_TYPE="$2"
            shift 2
            ;;
        --install-location)
            INSTALL_LOCATION="$2"
            shift 2
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

if [[ -z "$JAR_PATH" ]]; then
    echo "Error: --jar-path is required"
    print_usage
    exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
    echo "Error: JAR file not found: $JAR_PATH"
    exit 1
fi

# Auto-detect shell if not specified
if [[ -z "$SHELL_TYPE" ]]; then
    SHELL_TYPE=$(detect_shell)
    echo "Auto-detected shell: $SHELL_TYPE"
fi

# Validate shell type
if [[ "$SHELL_TYPE" != "bash" && "$SHELL_TYPE" != "zsh" ]]; then
    echo "Error: Unsupported shell type: $SHELL_TYPE"
    echo "Supported shells: bash, zsh"
    exit 1
fi

# Generate completion script
echo "Generating $SHELL_TYPE completion script..."
COMPLETION_SCRIPT=$(java -jar "$JAR_PATH" completion "$SHELL_TYPE")

if [[ $? -ne 0 ]]; then
    echo "Error: Failed to generate completion script"
    exit 1
fi

# Determine installation path
if [[ -z "$INSTALL_LOCATION" ]]; then
    case "$SHELL_TYPE" in
        bash)
            if [[ -d "/etc/bash_completion.d" ]]; then
                INSTALL_LOCATION="/etc/bash_completion.d/a4sync"
            else
                INSTALL_LOCATION="$HOME/.bash_completion.d/a4sync"
                mkdir -p "$(dirname "$INSTALL_LOCATION")"
            fi
            ;;
        zsh)
            # Try to find zsh completion directory
            if [[ -d "/usr/local/share/zsh/site-functions" ]]; then
                INSTALL_LOCATION="/usr/local/share/zsh/site-functions/_a4sync"
            elif [[ -d "$HOME/.zsh/completions" ]]; then
                INSTALL_LOCATION="$HOME/.zsh/completions/_a4sync"
            else
                INSTALL_LOCATION="$HOME/.zsh/completions/_a4sync"
                mkdir -p "$(dirname "$INSTALL_LOCATION")"
            fi
            ;;
    esac
fi

echo "Installing completion script to: $INSTALL_LOCATION"

# Check if we need sudo for installation
if [[ "$INSTALL_LOCATION" =~ ^/etc/ ]] || [[ "$INSTALL_LOCATION" =~ ^/usr/ ]]; then
    if [[ $EUID -ne 0 ]]; then
        echo "Installing to system directory requires sudo..."
        echo "$COMPLETION_SCRIPT" | sudo tee "$INSTALL_LOCATION" > /dev/null
    else
        echo "$COMPLETION_SCRIPT" > "$INSTALL_LOCATION"
    fi
else
    # User installation
    mkdir -p "$(dirname "$INSTALL_LOCATION")"
    echo "$COMPLETION_SCRIPT" > "$INSTALL_LOCATION"
fi

echo "Completion script installed successfully!"
echo ""

# Provide activation instructions
case "$SHELL_TYPE" in
    bash)
        echo "To activate completions, run one of the following:"
        echo "  # Option 1: Restart your shell"
        echo "  exec bash"
        echo ""
        echo "  # Option 2: Source the completion script"
        echo "  source $INSTALL_LOCATION"
        echo ""
        echo "  # Option 3: Add to ~/.bashrc (permanent)"
        echo "  echo 'source $INSTALL_LOCATION' >> ~/.bashrc"
        ;;
    zsh)
        echo "To activate completions:"
        echo "  # Option 1: Restart your shell"
        echo "  exec zsh"
        echo ""
        echo "  # Option 2: Reload completions"
        echo "  compinit"
        echo ""
        if [[ "$INSTALL_LOCATION" =~ \.zsh/completions ]]; then
            echo "  # Make sure ~/.zsh/completions is in your fpath"
            echo "  # Add this to ~/.zshrc if not already present:"
            echo "  fpath=(~/.zsh/completions \$fpath)"
        fi
        ;;
esac

echo ""
echo "After activation, you can use tab completion with a4sync commands!"
echo "Try: a4sync <TAB> or a4sync repo <TAB>"