package com.cmdai;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * cmdai — CLI command assistant powered by LLM.
 * Zero external dependencies. Pure Java 11+.
 *
 * Usage:
 *   cmdai "find files larger than 100MB"
 *   cmdai --shell powershell "list processes by memory"
 *   cmdai --explain "replace string in all .txt files"
 *   cmdai config set apiKey sk-xxx
 *   cmdai config get
 */
public class CmdAi {

    private static final String VERSION = "1.0.0";

    // ── Entry point ─────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            new CmdAi().run(args);
        } catch (Exception e) {
            System.err.println("[31mFatal: " + e.getMessage() + "[0m");
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String first = args[0];

        // ── Subcommands ─────────────────────────────────────────
        if (first.equals("--help") || first.equals("-h"))     { printUsage(); return; }
        if (first.equals("--version") || first.equals("-V"))  { System.out.println("cmdai " + VERSION); return; }
        if (first.equals("config"))                           { handleConfig(slice(args, 1)); return; }
        if (first.equals("history"))                          { handleHistory(slice(args, 1)); return; }

        // ── Default: ask for a command ──────────────────────────
        handleAsk(args);
    }

    // ── "ask" (default command) ─────────────────────────────────

    private void handleAsk(String[] args) throws Exception {
        String query       = null;
        String shellOpt    = null;
        String modelOpt    = null;
        boolean explainOpt = false;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--shell":
                case "-s":
                    shellOpt = nextArg(args, ++i, "--shell");
                    break;
                case "--model":
                case "-m":
                    modelOpt = nextArg(args, ++i, "--model");
                    break;
                case "--explain":
                case "-e":
                    explainOpt = true;
                    break;
                default:
                    if (query == null) {
                        query = args[i];
                    } else {
                        query += " " + args[i];
                    }
            }
        }

        if (query == null || query.isEmpty()) {
            System.err.println("Error: Please provide a query. Example:");
            System.err.println("  cmdai \"find all .log files larger than 100MB\"");
            System.exit(1);
        }

        ConfigManager config = new ConfigManager();
        String apiKey = config.getResolvedApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("No API key found. Configure one:");
            System.err.println("  cmdai config set apiKey <your-key>");
            System.err.println("Or set the CMDAI_API_KEY / OPENAI_API_KEY environment variable.");
            System.exit(1);
        }

        String shell  = (shellOpt != null) ? shellOpt : config.getShell();
        String model  = (modelOpt != null) ? modelOpt : config.getModel();

        System.out.println("[90mThinking...[0m");

        OpenAIClient client = new OpenAIClient(config.getBaseUrl(), model, apiKey);
        CommandResult result = client.ask(query, shell);

        // ── Display result ──────────────────────────────────────
        System.out.println();
        System.out.println("[1;36m  Command:[0m");
        System.out.println("[1;32m  " + result.getCommand() + "[0m");
        System.out.println();

        if (explainOpt && result.getExplanation() != null && !result.getExplanation().isEmpty()) {
            System.out.println("[1;36m  Explanation:[0m");
            System.out.println("  " + result.getExplanation());
            System.out.println();
        }

        // Show alternatives for other shells
        showAlternative("PowerShell", result.getPowershell(), shell);
        showAlternative("CMD",        result.getCmd(),        shell);
        showAlternative("fish",       result.getFish(),       shell);
        showAlternative("bash",       result.getBash(),       shell);

        // Show warnings
        if (result.getWarnings() != null && result.getWarnings().length > 0) {
            System.out.println("[1;33m  Warnings:[0m");
            for (String w : result.getWarnings()) {
                System.out.println("  [33m! " + w + "[0m");
            }
            System.out.println();
        }

        // Save to history
        appendHistory(query, result.getCommand());
    }

    private void showAlternative(String label, String cmd, String currentShell) {
        if (cmd == null || cmd.isEmpty()) return;
        String lower = label.toLowerCase();
        String current = currentShell.toLowerCase();
        if (lower.equals(current)
            || (lower.equals("bash") && (current.equals("bash") || current.equals("zsh")))
            || (lower.equals("cmd") && current.equals("cmd"))) {
            return;
        }
        System.out.println("[90m  " + label + ": " + cmd + "[0m");
    }

    // ── "config" subcommand ─────────────────────────────────────

    private void handleConfig(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: cmdai config <get|set|reset> [key] [value]");
            System.out.println();
            System.out.println("Commands:");
            System.out.println("  get  [key]         Show configuration (or a specific key)");
            System.out.println("  set  <key> <value> Set a configuration value");
            System.out.println("  reset              Reset to defaults");
            System.out.println();
            System.out.println("Keys: apiKey, baseUrl, model, shell");
            return;
        }

        ConfigManager config = new ConfigManager();
        String sub = args[0];

        switch (sub) {
            case "get":
                if (args.length >= 2) {
                    printConfigKey(config, args[1]);
                } else {
                    printAllConfig(config);
                }
                break;

            case "set":
                if (args.length < 3) {
                    System.err.println("Usage: cmdai config set <key> <value>");
                    System.exit(1);
                }
                setConfigKey(config, args[1], args[2]);
                break;

            case "reset":
                config.setApiKey(null);
                config.setBaseUrl("https://api.openai.com/v1");
                config.setModel("gpt-4o-mini");
                config.setShell("bash");
                config.save();
                System.out.println("Configuration reset to defaults.");
                break;

            default:
                System.err.println("Unknown config command: " + sub);
                System.exit(1);
        }
    }

    private void printAllConfig(ConfigManager config) {
        String key = config.getResolvedApiKey();
        String masked = (key != null) ? key.substring(0, Math.min(8, key.length())) + "..." : "(not set)";
        System.out.println("apiKey  : " + masked);
        System.out.println("baseUrl : " + config.getBaseUrl());
        System.out.println("model   : " + config.getModel());
        System.out.println("shell   : " + config.getShell());
        System.out.println();
        System.out.println("Config file: " + ConfigManager.getConfigPath());
    }

    private void printConfigKey(ConfigManager config, String key) {
        switch (key.toLowerCase()) {
            case "apikey":  System.out.println(config.getResolvedApiKey() != null ? config.getResolvedApiKey() : "(not set)"); break;
            case "baseurl": System.out.println(config.getBaseUrl()); break;
            case "model":   System.out.println(config.getModel());   break;
            case "shell":   System.out.println(config.getShell());   break;
            default:
                System.err.println("Unknown key: " + key + " (valid: apiKey, baseUrl, model, shell)");
                System.exit(1);
        }
    }

    private void setConfigKey(ConfigManager config, String key, String value) {
        switch (key.toLowerCase()) {
            case "apikey":  config.setApiKey(value);  break;
            case "baseurl": config.setBaseUrl(value);  break;
            case "model":   config.setModel(value);    break;
            case "shell":   config.setShell(value);    break;
            default:
                System.err.println("Unknown key: " + key + " (valid: apiKey, baseUrl, model, shell)");
                System.exit(1);
        }
        config.save();
        System.out.println("Saved " + key + " → " + ConfigManager.getConfigPath());
    }

    // ── "history" subcommand ────────────────────────────────────

    private void handleHistory(String[] args) throws Exception {
        String historyPath = Paths.get(System.getProperty("user.home"), ".cmdai_history").toString();
        Path path = Paths.get(historyPath);

        if (args.length > 0 && args[0].equals("--clear")) {
            Files.deleteIfExists(path);
            System.out.println("History cleared.");
            return;
        }

        if (!Files.exists(path)) {
            System.out.println("No history yet.");
            return;
        }

        List<String> lines = Files.readAllLines(path);
        int start = Math.max(0, lines.size() - 20);
        for (int i = start; i < lines.size(); i++) {
            System.out.println("  " + lines.get(i));
        }
    }

    private void appendHistory(String query, String command) {
        try {
            String historyPath = Paths.get(System.getProperty("user.home"), ".cmdai_history").toString();
            String line = java.time.LocalDateTime.now().toString().substring(0, 19)
                        + "  " + query + "  →  " + command;
            Files.write(Paths.get(historyPath), (line + "\n").getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Silently ignore history write failures
        }
    }

    // ── Utilities ───────────────────────────────────────────────

    private void printUsage() {
        System.out.println("cmdai " + VERSION + " — CLI command assistant powered by LLM");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  cmdai \"<describe what you want>\"    Get the command");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -s, --shell <shell>     Target shell (bash|zsh|powershell|cmd|fish)");
        System.out.println("  -m, --model <model>     Override the LLM model");
        System.out.println("  -e, --explain           Show detailed explanation");
        System.out.println("  -h, --help              Show this help");
        System.out.println("  -V, --version           Show version");
        System.out.println();
        System.out.println("Subcommands:");
        System.out.println("  config get [key]        Show configuration");
        System.out.println("  config set <key> <val>  Set a configuration value");
        System.out.println("  config reset            Reset to defaults");
        System.out.println("  history [--clear]       Show or clear command history");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  cmdai \"find files larger than 100MB\"");
        System.out.println("  cmdai --shell powershell \"list processes by memory\"");
        System.out.println("  cmdai --explain \"replace string in all .txt files\"");
        System.out.println("  cmdai config set apiKey sk-your-key");
        System.out.println("  cmdai config set baseUrl https://api.deepseek.com/v1");
    }

    private String nextArg(String[] args, int index, String flag) {
        if (index >= args.length) {
            System.err.println("Error: " + flag + " requires a value");
            System.exit(1);
        }
        return args[index];
    }

    private String[] slice(String[] arr, int from) {
        return Arrays.copyOfRange(arr, from, arr.length);
    }
}
