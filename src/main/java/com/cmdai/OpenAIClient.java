package com.cmdai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI-compatible API client — uses Java 11 HttpClient, zero dependencies.
 */
public class OpenAIClient {

    private static final String SYSTEM_PROMPT =
        "You are a CLI command expert. The user will describe a task in natural language.\n" +
        "Return ONLY a JSON object with these fields:\n" +
        "- \"command\": the most likely bash command for the task\n" +
        "- \"explanation\": a brief explanation (1-2 sentences)\n" +
        "- \"powershell\": the PowerShell equivalent (if different from command)\n" +
        "- \"cmd\": the Windows CMD equivalent (if different)\n" +
        "- \"fish\": the fish shell equivalent (if different)\n" +
        "- \"warnings\": array of safety warnings (e.g., for destructive operations)\n" +
        "Return ONLY valid JSON. No markdown fences, no extra text.";

    private final String apiUrl;
    private final String model;
    private final String apiKey;
    private final HttpClient http;

    public OpenAIClient(String baseUrl, String model, String apiKey) {
        this.apiUrl = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                      + "/chat/completions";
        this.model  = model;
        this.apiKey = apiKey;
        this.http   = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .build();
    }

    /** Send a query and return the parsed CommandResult. */
    public CommandResult ask(String query, String shellHint) throws Exception {
        String userMsg = query;
        if (shellHint != null && !shellHint.isEmpty()) {
            userMsg += " (I'm using " + shellHint + " shell)";
        }

        String body = JsonUtil.buildChatRequest(model, SYSTEM_PROMPT, userMsg);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        return parseResponse(response.body());
    }

    private CommandResult parseResponse(String responseBody) throws Exception {
        String content = JsonUtil.extractContent(responseBody);
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Empty response from API");
        }

        content = JsonUtil.stripCodeFences(content);

        // Parse the inner JSON from the LLM
        CommandResult result = new CommandResult();
        result.setCommand(JsonUtil.getString(content, "command"));
        result.setExplanation(JsonUtil.getString(content, "explanation"));
        result.setBash(JsonUtil.getString(content, "bash"));
        result.setPowershell(JsonUtil.getString(content, "powershell"));
        result.setCmd(JsonUtil.getString(content, "cmd"));
        result.setFish(JsonUtil.getString(content, "fish"));

        // If "command" is missing, use the entire content as fallback
        if (result.getCommand() == null || result.getCommand().isEmpty()) {
            result.setCommand(content.trim());
        }

        return result;
    }
}
