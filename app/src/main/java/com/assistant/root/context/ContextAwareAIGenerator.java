package com.assistant.root.context;

import android.content.Context;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Context-aware AI command generator
 * Enhances prompts with current screen context for smarter command generation
 */
public class ContextAwareAIGenerator {
    private static final String TAG = "ContextAwareAI";
    private GenerativeModelFutures model;
    private Context context;
    private Executor executor;

    public interface CommandCallback {
        void onCommandGenerated(String command);

        void onError(String error);
    }

    public ContextAwareAIGenerator(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        initializeAI();
    }

    private void initializeAI() {
        try {
            // Try different model names in order of preference
            String[] modelNames = {
                    "gemini-2.5-pro",
                    "gemini-1.5-pro",
                    "gemini-1.5-flash",
                    "gemini-1.0-pro"
            };

            String apiKey = com.assistant.root.BuildConfig.GOOGLE_AI_API_KEY;

            // Validate API key
            if (apiKey == null || apiKey.isEmpty()) {
                Log.e(TAG, "❌ Google AI API key not found! Please add GOOGLE_AI_API_KEY to local.properties");
                return;
            }

            for (String modelName : modelNames) {
                try {
                    Log.d(TAG, "Trying to initialize context-aware model: " + modelName);
                    GenerativeModel gm = new GenerativeModel(modelName, apiKey);
                    model = GenerativeModelFutures.from(gm);
                    Log.d(TAG, "Context-aware AI Model initialized successfully with: " + modelName);
                    break;
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize model " + modelName + ": " + e.getMessage());
                    if (modelName.equals(modelNames[modelNames.length - 1])) {
                        Log.e(TAG, "All models failed to initialize");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize context-aware AI: " + e.getMessage());
        }
    }

    /**
     * Generate context-aware command
     */
    public void generateContextAwareCommand(String userInput, CommandCallback callback) {
        if (model == null) {
            callback.onError("AI model not initialized");
            return;
        }

        // Get current context
        String contextSummary = ContextDetector.getContextSummary();
        String elementsSummary = UIElementParser.getElementsSummary();

        // Build enhanced prompt
        String prompt = buildContextAwarePrompt(userInput, contextSummary, elementsSummary);

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String generatedCommand = result.getText().trim();
                    Log.d(TAG, "Generated context-aware command: " + generatedCommand);

                    generatedCommand = cleanCommand(generatedCommand);

                    if (isSafeCommand(generatedCommand)) {
                        callback.onCommandGenerated(generatedCommand);
                    } else {
                        callback.onError("Generated command failed safety check");
                    }
                } catch (Exception e) {
                    callback.onError("Error processing AI response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "AI generation failed: " + t.getMessage());
                callback.onError("AI generation failed: " + t.getMessage());
            }
        }, executor);
    }

    /**
     * Build context-aware prompt for AI
     */
    private String buildContextAwarePrompt(String userInput, String context, String elements) {
        return "You are a context-aware Android automation assistant. Generate shell commands based on the CURRENT screen context.\n\n"
                +

                "═══ CURRENT CONTEXT ═══\n" +
                context + "\n\n" +

                "═══ AVAILABLE ELEMENTS ═══\n" +
                elements + "\n\n" +

                "═══ USER REQUEST ═══\n" +
                userInput + "\n\n" +

                "═══ IMPORTANT RULES ═══\n" +
                "1. User is ALREADY in the app mentioned above - DO NOT open the app again\n" +
                "2. Use ONLY elements that are visible on current screen\n" +
                "3. Generate commands to accomplish the task from CURRENT state\n" +
                "4. Return ONLY executable commands, one per line\n" +
                "5. Use 'input tap X Y' to click elements at their coordinates\n" +
                "6. Use 'input text' for typing (replace spaces with %s)\n" +
                "7. Add 'sleep 1' between steps for UI to load\n" +
                "8. If element is found, use its exact coordinates\n" +
                "9. NO explanations, NO markdown, ONLY commands\n\n" +

                "═══ COMMAND PATTERNS ═══\n" +
                "Click element: input tap <centerX> <centerY>\n" +
                "Type text: input text 'text%swith%sspaces'\n" +
                "Press back: input keyevent 4\n" +
                "Press enter: input keyevent 66\n" +
                "Wait: sleep 1\n\n" +

                "═══ EXAMPLE 1 ═══\n" +
                "Context: WhatsApp - Chats List\n" +
                "Elements: Search button at (950,150)\n" +
                "User: message John saying hello\n" +
                "Commands:\n" +
                "input tap 950 150\n" +
                "sleep 1\n" +
                "input text 'John'\n" +
                "sleep 1\n" +
                "input tap 540 400\n" +
                "sleep 1\n" +
                "input text 'hello'\n" +
                "sleep 0.5\n" +
                "input tap 950 1850\n\n" +

                "═══ EXAMPLE 2 ═══\n" +
                "Context: Google Maps - Map View\n" +
                "Elements: Search box at (540,200)\n" +
                "User: search restaurants near me\n" +
                "Commands:\n" +
                "input tap 540 200\n" +
                "sleep 1\n" +
                "input text 'restaurants%snear%sme'\n" +
                "sleep 0.5\n" +
                "input keyevent 66\n\n" +

                "Now generate commands for the user's request:\n" +
                "Commands:";
    }

    /**
     * Clean AI-generated command
     */
    private String cleanCommand(String command) {
        command = command.replaceAll("```[a-z]*\n?", "").trim();
        command = command.replaceFirst("^Commands?:\\s*", "");

        String[] lines = command.split("\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("input ") || line.startsWith("sleep ") ||
                    line.startsWith("am ") || line.startsWith("pm ")) {
                if (cleaned.length() > 0)
                    cleaned.append("\n");
                cleaned.append(line);
            }
        }

        return cleaned.toString().trim();
    }

    /**
     * Validate command safety
     */
    private boolean isSafeCommand(String command) {
        if (command == null || command.isEmpty())
            return false;

        String[] blockedPatterns = {
                "rm ", "dd ", "format", "flash", "fastboot",
                "&&", "||", ";", "|", "reboot", "shutdown",
                "chmod 777", "su -c"
        };

        String lowerCommand = command.toLowerCase();
        for (String pattern : blockedPatterns) {
            if (lowerCommand.contains(pattern.toLowerCase())) {
                return false;
            }
        }

        String[] allowedPrefixes = { "input ", "sleep ", "am ", "pm " };
        String[] lines = command.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            boolean valid = false;
            for (String prefix : allowedPrefixes) {
                if (line.startsWith(prefix)) {
                    valid = true;
                    break;
                }
            }
            if (!valid)
                return false;
        }

        return true;
    }
}
