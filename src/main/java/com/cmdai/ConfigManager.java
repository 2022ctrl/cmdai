package com.cmdai;

import java.io.*;
import java.nio.file.*;

/**
 * Manages configuration from ~/.cmdai.json — pure standard library.
 */
public class ConfigManager {

    private static final String CONFIG_FILE = ".cmdai.json";
    private static final String CONFIG_DIR  = ".cmdai";

    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private String model   = "gpt-4o-mini";
    private String shell   = "bash";

    public ConfigManager() {
        load();
    }

    public static String getConfigPath() {
        return Paths.get(System.getProperty("user.home"), CONFIG_FILE).toString();
    }

    // ── Load / Save (manual JSON, no libraries) ────────────────

    private void load() {
        Path path = Paths.get(getConfigPath());
        if (!Files.exists(path)) return;

        try {
            String raw = new String(Files.readAllBytes(path));
            apiKey  = extractJsonString(raw, "apiKey");
            String v;

            v = extractJsonString(raw, "baseUrl");
            if (v != null) baseUrl = v;

            v = extractJsonString(raw, "model");
            if (v != null) model = v;

            v = extractJsonString(raw, "shell");
            if (v != null) shell = v;
        } catch (IOException e) {
            System.err.println("Warning: Could not read config: " + e.getMessage());
        }
    }

    public void save() {
        StringBuilder sb = new StringBuilder("{\n");
        if (apiKey != null)  sb.append("  \"apiKey\":  ").append(JsonUtil.escape(apiKey)).append(",\n");
        sb.append("  \"baseUrl\": ").append(JsonUtil.escape(baseUrl)).append(",\n");
        sb.append("  \"model\":   ").append(JsonUtil.escape(model)).append(",\n");
        sb.append("  \"shell\":   ").append(JsonUtil.escape(shell)).append("\n");
        sb.append("}");

        try {
            Files.write(Paths.get(getConfigPath()), sb.toString().getBytes());
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    /** Simple extraction of a top-level string value from raw JSON. */
    private static String extractJsonString(String raw, String key) {
        String pattern = "\"" + key + "\"";
        int idx = raw.indexOf(pattern);
        if (idx < 0) return null;

        int i = idx + pattern.length();
        while (i < raw.length() && (raw.charAt(i) == ' ' || raw.charAt(i) == ':' || raw.charAt(i) == '\t')) i++;
        if (i >= raw.length() || raw.charAt(i) != '"') return null;
        i++;

        StringBuilder sb = new StringBuilder();
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                sb.append(raw.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ── API key resolution ──────────────────────────────────────

    public String getResolvedApiKey() {
        if (apiKey != null && !apiKey.isEmpty()) return apiKey;

        String env = System.getenv("CMDAI_API_KEY");
        if (env != null && !env.isEmpty()) return env;

        env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isEmpty()) return env;

        return null;
    }

    // ── Getters / Setters ───────────────────────────────────────

    public String getApiKey()              { return apiKey; }
    public void   setApiKey(String v)      { this.apiKey = v; }
    public String getBaseUrl()             { return baseUrl; }
    public void   setBaseUrl(String v)     { this.baseUrl = v; }
    public String getModel()               { return model; }
    public void   setModel(String v)       { this.model = v; }
    public String getShell()               { return shell; }
    public void   setShell(String v)       { this.shell = v; }
}
