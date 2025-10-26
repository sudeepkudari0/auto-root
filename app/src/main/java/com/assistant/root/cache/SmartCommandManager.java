package com.assistant.root.cache;

import android.content.Context;
import android.util.Log;

import com.assistant.root.ai.AICommandGenerator;

/**
 * Smart Command Manager with caching and predictive AI fallback
 * Eliminates 5-10 second delay for common commands
 */
public class SmartCommandManager {
    private static final String TAG = "SmartCommandManager";

    private HybridCommandCache cache;
    private AICommandGenerator aiGenerator;
    private Context context;

    public interface CommandCallback {
        void onCommandReady(String command, boolean fromCache);

        void onError(String error);
    }

    public SmartCommandManager(Context context) {
        this.context = context;
        this.cache = new HybridCommandCache(context);
        this.aiGenerator = new AICommandGenerator(context);
    }

    /**
     * Get command - checks cache first, then AI
     * INSTANT for cached commands, 5-10s for new commands
     */
    public void getCommand(String userInput, CommandCallback callback) {
        // Step 1: Check cache first (INSTANT)
        HybridCommandCache.CachedCommand cached = cache.get(userInput);

        if (cached != null) {
            Log.d(TAG, "✓ Cache hit! Instant execution");
            callback.onCommandReady(cached.command, true);
            return;
        }

        // Step 2: Not in cache, use AI (5-10 seconds)
        Log.d(TAG, "✗ Cache miss, calling AI...");
        aiGenerator.generateCommand(userInput, new AICommandGenerator.CommandCallback() {
            @Override
            public void onCommandGenerated(String command) {
                // Store in cache for next time
                cache.put(userInput, command);
                Log.d(TAG, "AI generated and cached command");
                callback.onCommandReady(command, false);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Pre-generate commands in background for predicted actions
     * Reduces perceived latency
     */
    public void preloadPredictedCommands(String currentContext) {
        new Thread(() -> {
            // Based on current context, predict next actions
            String[] predictions = predictNextCommands(currentContext);

            for (String prediction : predictions) {
                if (cache.get(prediction) == null) {
                    Log.d(TAG, "Preloading: " + prediction);
                    aiGenerator.generateCommand(prediction, new AICommandGenerator.CommandCallback() {
                        @Override
                        public void onCommandGenerated(String command) {
                            cache.put(prediction, command);
                        }

                        @Override
                        public void onError(String error) {
                            // Ignore errors for predictions
                        }
                    });

                    try {
                        Thread.sleep(2000); // Throttle API calls
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
    }

    /**
     * Predict likely next commands based on context
     */
    private String[] predictNextCommands(String currentContext) {
        String lower = currentContext.toLowerCase();

        // If user opened WhatsApp, they might send a message next
        if (lower.contains("whatsapp")) {
            return new String[] {
                    "send message to mom",
                    "send message to dad",
                    "open status",
                    "go back"
            };
        }

        // If user opened YouTube, they might search
        if (lower.contains("youtube")) {
            return new String[] {
                    "search music",
                    "search funny videos",
                    "open subscriptions",
                    "go back"
            };
        }

        // If user is typing, they might want to edit
        if (lower.contains("type")) {
            return new String[] {
                    "delete text",
                    "press enter",
                    "go back"
            };
        }

        // Default predictions
        return new String[] {
                "go back",
                "go home",
                "open settings"
        };
    }

    /**
     * Batch generate and cache multiple commands
     * Run this on app startup in background
     */
    public void warmupCache() {
        Log.d(TAG, "Warming up cache with common commands...");

        String[] commonCommands = {
                "open whatsapp",
                "open youtube",
                "open instagram",
                "open chrome",
                "go back",
                "go home",
                "take screenshot",
                "open settings",
                "open camera",
                "search google for weather"
        };

        new Thread(() -> {
            for (String cmd : commonCommands) {
                if (cache.get(cmd) == null) {
                    try {
                        Thread.sleep(1000); // Throttle
                        // Generate and cache
                        getCommand(cmd, new CommandCallback() {
                            @Override
                            public void onCommandReady(String command, boolean fromCache) {
                                Log.d(TAG, "Warmed up: " + cmd);
                            }

                            @Override
                            public void onError(String error) {
                                Log.w(TAG, "Warmup failed for: " + cmd);
                            }
                        });
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            Log.d(TAG, "Cache warmup complete!");
        }).start();
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return cache.getStats();
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        cache.clearAll();
    }

    /**
     * Clean old entries
     */
    public void cleanupCache() {
        cache.cleanup();
    }
}
