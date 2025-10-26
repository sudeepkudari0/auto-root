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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HybridAIGenerator {
    private static final String TAG = "HybridAI";
    private GenerativeModelFutures model;
    private Executor executor;

    public interface AICallback {
        void onGenerated(String command);

        void onError(String error);
    }

    public HybridAIGenerator(Context context) {
        this.executor = Executors.newSingleThreadExecutor();
        initAI();
    }

    private void initAI() {
        try {
            // Try multiple model names for compatibility
            String[] modelNames = { "gemini-1.5-flash", "gemini-1.5-pro", "gemini-pro", "gemini-1.0-pro" };
            GenerativeModel gm = null;

            for (String modelName : modelNames) {
                try {
                    gm = new GenerativeModel(modelName, com.assistant.root.BuildConfig.GOOGLE_AI_API_KEY);
                    Log.d(TAG, "AI initialized with model: " + modelName);
                    break;
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize model " + modelName + ": " + e.getMessage());
                }
            }

            if (gm != null) {
                model = GenerativeModelFutures.from(gm);
                Log.d(TAG, "AI model ready");
            } else {
                Log.e(TAG, "Failed to initialize any AI model");
            }
        } catch (Exception e) {
            Log.e(TAG, "AI init failed: " + e.getMessage());
        }
    }

    public void generateWithContext(String userInput,
            ContextDetector.AppContext appContext,
            List<UIElementParser.UIElement> elements,
            AICallback callback) {
        if (model == null) {
            callback.onError("AI not initialized");
            return;
        }

        String prompt = buildPrompt(userInput, appContext, elements);

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String command = cleanCommand(result.getText());
                if (isValid(command)) {
                    callback.onGenerated(command);
                } else {
                    callback.onError("Invalid command generated");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError("AI failed: " + t.getMessage());
            }
        }, executor);
    }

    private String buildPrompt(String userInput, ContextDetector.AppContext ctx,
            List<UIElementParser.UIElement> elements) {
        return "Generate Android shell commands.\n\n" +
                "CONTEXT:\n" +
                "App: " + ctx.appName + "\n" +
                "Screen: " + ctx.screenDescription + "\n\n" +
                "ELEMENTS:\n" +
                UIElementParser.formatForAI(elements) + "\n" +
                "USER: " + userInput + "\n\n" +
                "RULES:\n" +
                "- Return ONLY commands, one per line\n" +
                "- Use 'input tap X Y' for clicks\n" +
                "- Use 'input text' for typing (spaces as %s)\n" +
                "- Add 'sleep 1' between steps\n" +
                "- No explanations\n\n" +
                "Commands:";
    }

    private String cleanCommand(String cmd) {
        return cmd.replaceAll("```[a-z]*\n?", "")
                .replaceFirst("^Commands?:\\s*", "")
                .trim();
    }

    private boolean isValid(String cmd) {
        return cmd != null && !cmd.isEmpty() &&
                (cmd.startsWith("input ") || cmd.startsWith("sleep "));
    }
}
