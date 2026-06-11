package com.cmdai;

/**
 * Minimal JSON builder and parser — zero dependencies.
 * Only handles the flat structures needed for OpenAI API calls.
 */
public class JsonUtil {

    // ── Build JSON strings ──────────────────────────────────────

    public static String escape(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t") + "\"";
    }

    /** Build the OpenAI chat completion request body. */
    public static String buildChatRequest(String model, String systemPrompt, String userMsg) {
        return "{"
             + "\"model\":" + escape(model) + ","
             + "\"temperature\":0.1,"
             + "\"messages\":["
             +   "{\"role\":\"system\",\"content\":" + escape(systemPrompt) + "},"
             +   "{\"role\":\"user\",\"content\":" + escape(userMsg) + "}"
             + "]"
             + "}";
    }

    // ── Parse JSON (good enough for OpenAI responses) ───────────

    private static String json;

    /** Parse a top-level field value as a string. */
    public static String getString(String rawJson, String key) {
        json = rawJson.trim();
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;

        // skip past "key" and any whitespace/colon
        int i = idx + pattern.length();
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == ':' || json.charAt(i) == '\t')) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++; // skip opening quote

        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case 'n':  sb.append('\n'); i += 2; continue;
                    case 'r':  sb.append('\r'); i += 2; continue;
                    case 't':  sb.append('\t'); i += 2; continue;
                    default:   sb.append(c);   i++;    continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** Extract the "choices[0].message.content" field. */
    public static String extractContent(String responseBody) {
        // Navigate to choices[0].message.content
        int choicesIdx = responseBody.indexOf("\"choices\"");
        if (choicesIdx < 0) return null;

        int msgIdx = responseBody.indexOf("\"message\"", choicesIdx);
        if (msgIdx < 0) return null;

        int contentIdx = responseBody.indexOf("\"content\"", msgIdx);
        if (contentIdx < 0) return null;

        // Move past "content" and colon
        int i = contentIdx + "\"content\"".length();
        while (i < responseBody.length() && (responseBody.charAt(i) == ' ' || responseBody.charAt(i) == ':' || responseBody.charAt(i) == '\t')) {
            i++;
        }
        if (i >= responseBody.length() || responseBody.charAt(i) != '"') return null;
        i++; // skip opening quote

        StringBuilder sb = new StringBuilder();
        while (i < responseBody.length()) {
            char c = responseBody.charAt(i);
            if (c == '\\' && i + 1 < responseBody.length()) {
                char next = responseBody.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case 'n':  sb.append('\n'); i += 2; continue;
                    case 'r':  sb.append('\r'); i += 2; continue;
                    case 't':  sb.append('\t'); i += 2; continue;
                    default:   sb.append(c);   i++;    continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** Strip markdown code fences from LLM output. */
    public static String stripCodeFences(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return s;
    }
}
