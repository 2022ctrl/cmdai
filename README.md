# cmdai

CLI command assistant powered by LLM. Describe what you want in natural language, get the exact shell command.

## Features

- Natural language → shell command (bash, zsh, PowerShell, CMD, fish)
- Cross-platform: Linux, macOS, Windows
- Works with any OpenAI-compatible API (OpenAI, DeepSeek, Groq, Ollama, etc.)
- Shows alternative commands for other shells
- Safety warnings for destructive operations
- Zero external dependencies — pure Java 11+

## Quick Start

### Prerequisites

- JDK 11+ (needed only to build; runtime only needs JRE)

### Build

```bash
git clone https://github.com/yourname/cmdai.git
cd cmdai
chmod +x build.sh
./build.sh
```

On Windows:
```
build.bat
```

This produces `cmdai.jar`.

### Configure

```bash
# Set your API key
java -jar cmdai.jar config set apiKey sk-your-key-here

# Or use environment variable
export CMDAI_API_KEY=sk-your-key-here

# Set default shell
java -jar cmdai.jar config set shell bash

# Set model (default: gpt-4o-mini)
java -jar cmdai.jar config set model deepseek-chat

# Set custom API base URL
java -jar cmdai.jar config set baseUrl https://api.deepseek.com/v1
```

### Install (optional)

```bash
# Linux / macOS
sudo cp cmdai.jar /usr/local/lib/
echo 'alias cmdai="java -jar /usr/local/lib/cmdai.jar"' >> ~/.bashrc
source ~/.bashrc

# Now just use:
cmdai "find all .log files larger than 100MB"
```

## Usage

```bash
# Basic
cmdai "find all PNG files in current directory"

# Specify shell
cmdai --shell powershell "list running processes sorted by memory"

# Show explanation
cmdai --explain "find and replace a string in all .txt files"

# Use a specific model
cmdai --model deepseek-chat "compress all PNG files in current directory"

# View config
cmdai config get

# View history
cmdai history
```

## Supported API Providers

Any provider with an OpenAI-compatible API:

| Provider   | Base URL                            |
|------------|-------------------------------------|
| OpenAI     | `https://api.openai.com/v1`         |
| DeepSeek   | `https://api.deepseek.com/v1`       |
| Groq       | `https://api.groq.com/openai/v1`    |
| Together   | `https://api.together.xyz/v1`       |
| Ollama     | `http://localhost:11434/v1`         |

## Examples

```
$ cmdai "find all .log files larger than 100MB"

  Command:
  find . -name "*.log" -size +100M

$ cmdai --shell powershell "kill process on port 8080"

  Command:
  Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess }

$ cmdai --explain "delete all node_modules recursively"

  Command:
  find . -name "node_modules" -type d -exec rm -rf {} +

  Explanation:
  Finds all directories named 'node_modules' and recursively deletes them.

  Warnings:
  ! This will permanently delete all node_modules directories
```

## Environment Variables

| Variable          | Description                            |
|-------------------|----------------------------------------|
| `CMDAI_API_KEY`   | API key (overrides config file)        |
| `OPENAI_API_KEY`  | Fallback API key                       |

## Project Structure

```
cmdai/
├── build.sh              # Build script (Linux/macOS)
├── build.bat             # Build script (Windows)
├── src/main/java/com/cmdai/
│   ├── CmdAi.java        # Main CLI entry point
│   ├── OpenAIClient.java # OpenAI-compatible API client
│   ├── ConfigManager.java# Config file management
│   ├── CommandResult.java# Result model
│   └── JsonUtil.java     # Minimal JSON parser (zero deps)
├── .gitignore
└── README.md
```

## License

MIT
