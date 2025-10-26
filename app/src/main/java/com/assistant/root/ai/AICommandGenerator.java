package com.assistant.root.ai;

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

public class AICommandGenerator {
    private static final String TAG = "AICommandGenerator";
    private GenerativeModelFutures model;
    private Context context;
    private Executor executor;

    // Callback interface for command generation
    public interface CommandCallback {
        void onCommandGenerated(String command);

        void onError(String error);
    }

    public AICommandGenerator(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        initializeAI();
    }

    private void initializeAI() {
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

        Log.d(TAG, "✅ Google AI API key loaded successfully");

        for (String modelName : modelNames) {
            try {
                Log.d(TAG, "Trying to initialize model: " + modelName);
                GenerativeModel gm = new GenerativeModel(modelName, apiKey);
                model = GenerativeModelFutures.from(gm);
                Log.d(TAG, "AI Model initialized successfully with: " + modelName);
                return; // Success, exit the loop
            } catch (Exception e) {
                Log.w(TAG, "Failed to initialize model " + modelName + ": " + e.getMessage());
                // Continue to next model
            }
        }

        Log.e(TAG, "Failed to initialize any AI model");
    }

    /**
     * Check if AI model is available
     */
    public boolean isModelAvailable() {
        return model != null;
    }

    /**
     * Test AI model with a simple command
     */
    public void testAI(String testInput, CommandCallback callback) {
        if (model == null) {
            callback.onError("AI model not initialized");
            return;
        }

        String simplePrompt = "Convert this to a simple Android command: " + testInput + "\nCommand:";

        Content content = new Content.Builder()
                .addText(simplePrompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String generatedCommand = result.getText().trim();
                    Log.d(TAG, "Test AI response: " + generatedCommand);
                    callback.onCommandGenerated(generatedCommand);
                } catch (Exception e) {
                    callback.onError("Error processing test response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Test AI failed: " + t.getMessage());
                callback.onError("Test AI failed: " + t.getMessage());
            }
        }, executor);
    }

    /**
     * Generate Android shell command from natural language input
     */
    public void generateCommand(String userInput, CommandCallback callback) {
        if (model == null) {
            callback.onError("AI model not initialized");
            return;
        }

        // Create enhanced prompt for command generation
        String prompt = buildCommandPrompt(userInput);

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String generatedCommand = result.getText().trim();
                    Log.d(TAG, "Generated command: " + generatedCommand);

                    // Clean up the response
                    generatedCommand = cleanCommand(generatedCommand);

                    // Validate command before returning
                    if (isSafeCommand(generatedCommand)) {
                        callback.onCommandGenerated(generatedCommand);
                    } else {
                        callback.onError("Generated command failed safety check: " + generatedCommand);
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
     * Build comprehensive prompt for command generation
     */
    private String buildCommandPrompt(String userInput) {
        return "You are an Android shell command generator. Convert the user's natural language request into exact Android shell commands.\n\n"
                +
                "RULES:\n" +
                "1. Return ONLY the shell commands, nothing else\n" +
                "2. Use one command per line if multiple commands needed\n" +
                "3. Use standard Android commands: am, pm, input, settings\n" +
                "4. For delays between commands, use: sleep 2\n" +
                "5. No explanations, no markdown, no code blocks\n" +
                "6. PREFER direct URL commands over coordinate-based automation\n" +
                "7. For WhatsApp messaging, use URL scheme instead of tapping coordinates\n" +
                "8. Only use input tap when no direct command is available\n\n" +

                "COMMON PATTERNS:\n" +
                "- Open app: am start -n PACKAGE_NAME/ACTIVITY_NAME\n" +
                "- Open WhatsApp: am start -n com.whatsapp/.HomeActivity\n" +
                "- Open YouTube: am start -n com.google.android.youtube/.HomeActivity\n" +
                "- Open Chrome: am start -n com.android.chrome/com.google.android.apps.chrome.Main\n" +
                "- Open Instagram: am start -n com.instagram.android/.activity.MainTabActivity\n" +
                "- Type text: input text 'your_text_here'\n" +
                "- Tap screen: input tap X Y\n" +
                "- Press back: input keyevent 4\n" +
                "- Press home: input keyevent 3\n" +
                "- Press enter: input keyevent 66\n" +
                "- Open URL: am start -a android.intent.action.VIEW -d 'URL'\n" +
                "- Send WhatsApp message: am start -a android.intent.action.VIEW -d 'https://api.whatsapp.com/send?phone=PHONE&text=MESSAGE'\n"
                +
                "- Search YouTube: am start -a android.intent.action.SEARCH -n com.google.android.youtube/.activities.ShellActivity --es query 'SEARCH_TERM'\n"
                +
                "- Open trending YouTube: am start -a android.intent.action.VIEW -d 'vnd.youtube://www.youtube.com/feed/trending'\n\n"
                +
                "WHATSAPP SPECIFIC COMMANDS:\n" +
                "- Send message to contact: am start -a android.intent.action.VIEW -d 'https://api.whatsapp.com/send?phone=PHONE_NUMBER&text=MESSAGE'\n"
                +
                "- Open WhatsApp chat with contact: am start -a android.intent.action.VIEW -d 'https://api.whatsapp.com/send?phone=PHONE_NUMBER'\n"
                +
                "- Open WhatsApp: am start -n com.whatsapp/.HomeActivity\n" +
                "- Open WhatsApp status: am start -n com.whatsapp/.StatusActivity\n" +
                "- Open WhatsApp calls: am start -n com.whatsapp/.CallsActivity\n\n"
                +
                "CONTACT HANDLING:\n" +
                "- Use contact names directly in WhatsApp URL commands\n" +
                "- Example: 'https://api.whatsapp.com/send?phone=CONTACT_NAME&text=MESSAGE'\n" +
                "- Contact names will be automatically replaced with phone numbers\n" +
                "- Use the exact contact name as mentioned by the user\n\n"
                +

                "EXAMPLES:\n" +
                "User: 'open whatsapp'\n" +
                "Command: am start -n com.whatsapp/.HomeActivity\n\n" +

                "User: 'send hi to +919876543210 on whatsapp'\n" +
                "Command: am start -a android.intent.action.VIEW -d 'https://api.whatsapp.com/send?phone=919876543210&text=hi'\n\n"
                +

                "User: 'send message to devraj saying hi'\n" +
                "Command: am start -a android.intent.action.VIEW -d 'https://api.whatsapp.com/send?phone=devraj&text=hi'\n\n"
                +

                "User: 'open whatsapp and send message to devraj saying hi'\n" +
                "Command: am start -a android.intent.action.VIEW -d 'https://api.whatsapp.com/send?phone=devraj&text=hi'\n\n"
                +

                "User: 'open youtube and search for music'\n" +
                "Command: am start -a android.intent.action.SEARCH -n com.google.android.youtube/.activities.ShellActivity --es query 'music'\n\n"
                +

                "User: 'type hello world'\n" +
                "Command: input text 'hello%sworld'\n\n" +

                "User request: " + userInput + "\n" +
                "Command:";
    }

    /**
     * Clean up AI-generated command
     */
    private String cleanCommand(String command) {
        // Remove markdown code blocks
        command = command.replaceAll("```[a-z]*\n?", "").trim();

        // Remove common prefixes
        command = command.replaceFirst("^Command:\\s*", "");
        command = command.replaceFirst("^Shell command:\\s*", "");

        // Remove explanatory text (keep only command lines)
        String[] lines = command.split("\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            // Keep lines that look like commands
            if (line.startsWith("am ") || line.startsWith("pm ") ||
                    line.startsWith("input ") || line.startsWith("sleep ") ||
                    line.startsWith("settings ") || line.isEmpty()) {
                if (cleaned.length() > 0)
                    cleaned.append("\n");
                cleaned.append(line);
            }
        }

        return cleaned.toString().trim();
    }

    /**
     * Validate command for safety
     */
    private boolean isSafeCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        // Dangerous command patterns to block
        String[] blockedPatterns = {
                "rm ", "dd ", "format", "flash", "fastboot",
                "recovery", ":/ ", ">/", "rmdir", "del ",
                "&&", "||", ";", "|", "$(", "`",
                "chmod 777", "su -c", "reboot", "shutdown"
        };

        String lowerCommand = command.toLowerCase();
        for (String pattern : blockedPatterns) {
            if (lowerCommand.contains(pattern.toLowerCase())) {
                Log.w(TAG, "Blocked dangerous pattern: " + pattern);
                return false;
            }
        }

        // Allowed command prefixes
        String[] allowedPrefixes = { "am ", "pm ", "input ", "sleep ", "settings " };

        String[] lines = command.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            boolean hasAllowedPrefix = false;
            for (String prefix : allowedPrefixes) {
                if (line.startsWith(prefix)) {
                    hasAllowedPrefix = true;
                    break;
                }
            }

            if (!hasAllowedPrefix) {
                Log.w(TAG, "Command line has no allowed prefix: " + line);
                return false;
            }
        }

        return true;
    }

    /**
     * Get package name for common apps
     */
    public String getPackageName(String appName) {
        appName = appName.toLowerCase().trim();
        switch (appName) {
            case "whatsapp":
                return "com.whatsapp";
            case "youtube":
                return "com.google.android.youtube";
            case "chrome":
                return "com.android.chrome";
            case "instagram":
                return "com.instagram.android";
            case "facebook":
                return "com.facebook.katana";
            case "twitter":
            case "x":
                return "com.twitter.android";
            case "telegram":
                return "org.telegram.messenger";
            case "gmail":
                return "com.google.android.gm";
            case "maps":
                return "com.google.android.apps.maps";
            case "settings":
                return "com.android.settings";
            default:
                return null;
        }
    }

}
