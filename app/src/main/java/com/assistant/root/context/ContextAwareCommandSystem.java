package com.assistant.root.context;

import android.content.Context;
import android.util.Log;

import com.assistant.root.cache.HybridCommandCache;

/**
 * Main Context-Aware Command System
 * Integrates all components for intelligent command generation and execution
 */
public class ContextAwareCommandSystem {
    private static final String TAG = "ContextAwareSystem";

    private Context context;
    private ContextAwareAIGenerator aiGenerator;
    private HybridCommandCache cache;

    public interface SystemCallback {
        void onCommandReady(String command, boolean fromCache, String contextInfo);

        void onExecutionProgress(String step);

        void onExecutionComplete(String result);

        void onError(String error);
    }

    public ContextAwareCommandSystem(Context context) {
        this.context = context;
        this.aiGenerator = new ContextAwareAIGenerator(context);
        this.cache = new HybridCommandCache(context);
    }

    /**
     * Main entry point - Process user input with full context awareness
     */
    public void processCommand(String userInput, SystemCallback callback) {
        new Thread(() -> {
            try {
                // Step 1: Detect current context
                ContextDetector.AppContext appContext = ContextDetector.getCurrentContext();

                if (appContext == null) {
                    callback.onError("Could not detect current app context");
                    return;
                }

                String contextInfo = appContext.toString();
                Log.d(TAG, "Context: " + contextInfo);

                // Step 2: Build cache key with context
                String cacheKey = buildContextualCacheKey(userInput, appContext);

                // Step 3: Check cache first
                HybridCommandCache.CachedCommand cached = cache.get(cacheKey);

                if (cached != null) {
                    Log.d(TAG, "✓ Cache HIT for context-aware command");
                    callback.onCommandReady(cached.command, true, contextInfo);
                    return;
                }

                // Step 4: Generate context-aware command with AI
                Log.d(TAG, "✗ Cache MISS, generating context-aware command...");

                aiGenerator.generateContextAwareCommand(userInput,
                        new ContextAwareAIGenerator.CommandCallback() {
                            @Override
                            public void onCommandGenerated(String command) {
                                // Cache the generated command
                                cache.put(cacheKey, command);
                                Log.d(TAG, "Command generated and cached");
                                callback.onCommandReady(command, false, contextInfo);
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError(error);
                            }
                        });

            } catch (Exception e) {
                callback.onError("System error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Execute command with smart execution
     */
    public void executeCommand(String command, SystemCallback callback) {
        SmartCommandExecutor.executeWithProgress(command,
                new SmartCommandExecutor.ExecutionCallback() {
                    @Override
                    public void onSuccess(String output) {
                        callback.onExecutionComplete("Success: " + output);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Execution failed: " + error);
                    }

                    @Override
                    public void onProgress(String step) {
                        callback.onExecutionProgress(step);
                    }
                });
    }

    /**
     * Quick execute - combines processing and execution
     */
    public void quickExecute(String userInput, SystemCallback callback) {
        processCommand(userInput, new SystemCallback() {
            @Override
            public void onCommandReady(String command, boolean fromCache, String contextInfo) {
                callback.onCommandReady(command, fromCache, contextInfo);
                // Auto-execute after command is ready
                executeCommand(command, callback);
            }

            @Override
            public void onExecutionProgress(String step) {
                callback.onExecutionProgress(step);
            }

            @Override
            public void onExecutionComplete(String result) {
                callback.onExecutionComplete(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Build contextual cache key
     * Same command in different apps = different cache entries
     */
    private String buildContextualCacheKey(String userInput, ContextDetector.AppContext context) {
        return context.packageName + ":" +
                context.activityName + ":" +
                userInput.toLowerCase().trim();
    }

    /**
     * Try quick template first before AI
     * Faster for common actions
     */
    public void smartProcess(String userInput, SystemCallback callback) {
        new Thread(() -> {
            // Get current context
            ContextDetector.AppContext appContext = ContextDetector.getCurrentContext();

            if (appContext == null) {
                // Fallback to regular processing
                processCommand(userInput, callback);
                return;
            }

            // Try to get quick template
            String template = AppContextHandler.getQuickCommand(appContext.packageName, userInput);

            if (template != null) {
                Log.d(TAG, "✓ Using quick template");
                callback.onCommandReady(template, true, appContext.toString());
                return;
            }

            // No template, use full AI processing
            processCommand(userInput, callback);
        }).start();
    }

    /**
     * Get current context information
     */
    public String getCurrentContextInfo() {
        return ContextDetector.getContextSummary();
    }

    /**
     * Get available actions in current context
     */
    public String getAvailableActions() {
        return UIElementParser.getElementsSummary();
    }

    /**
     * Get app-specific help
     */
    public String getAppHelp() {
        ContextDetector.AppContext context = ContextDetector.getCurrentContext();
        if (context != null) {
            return AppContextHandler.getAppHelp(context.packageName);
        }
        return "No app context detected";
    }

    /**
     * Get common actions for current app
     */
    public String[] getCommonActions() {
        ContextDetector.AppContext context = ContextDetector.getCurrentContext();
        if (context != null) {
            return AppContextHandler.getCommonActions(context.packageName);
        }
        return new String[] { "search", "back", "home" };
    }

    /**
     * Check if current app supports quick commands
     */
    public boolean supportsQuickCommands() {
        ContextDetector.AppContext context = ContextDetector.getCurrentContext();
        if (context != null) {
            return AppContextHandler.supportsQuickCommands(context.packageName);
        }
        return false;
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        cache.clearAll();
    }

    /**
     * Get cache stats
     */
    public String getCacheStats() {
        return cache.getStats();
    }

    /**
     * Cleanup old cache entries
     */
    public void cleanupCache() {
        cache.cleanup();
    }

    /**
     * Warm up cache with common commands for current app
     */
    public void warmupCurrentAppCache() {
        ContextDetector.AppContext context = ContextDetector.getCurrentContext();
        if (context != null && AppContextHandler.supportsQuickCommands(context.packageName)) {
            String[] commonActions = AppContextHandler.getCommonActions(context.packageName);

            new Thread(() -> {
                for (String action : commonActions) {
                    String cacheKey = buildContextualCacheKey(action, context);
                    if (cache.get(cacheKey) == null) {
                        // Generate and cache this command
                        aiGenerator.generateContextAwareCommand(action,
                                new ContextAwareAIGenerator.CommandCallback() {
                                    @Override
                                    public void onCommandGenerated(String command) {
                                        cache.put(cacheKey, command);
                                        Log.d(TAG, "Warmed up cache for: " + action);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.w(TAG, "Failed to warm up cache for: " + action);
                                    }
                                });

                        try {
                            Thread.sleep(2000); // Throttle API calls
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                Log.d(TAG, "Cache warmup complete for " + context.appName);
            }).start();
        }
    }

    /**
     * Get system status
     */
    public String getSystemStatus() {
        ContextDetector.AppContext context = ContextDetector.getCurrentContext();
        StringBuilder status = new StringBuilder();

        status.append("=== Context-Aware System Status ===\n");

        if (context != null) {
            status.append("Current App: ").append(context.appName).append("\n");
            status.append("Screen: ").append(context.screenDescription).append("\n");
            status.append("Package: ").append(context.packageName).append("\n");
            status.append("Quick Commands: ").append(supportsQuickCommands() ? "Supported" : "Not Supported")
                    .append("\n");
        } else {
            status.append("Context: Not detected\n");
        }

        status.append("Cache: ").append(getCacheStats()).append("\n");
        status.append("AI: ").append(aiGenerator != null ? "Ready" : "Not Ready").append("\n");

        return status.toString();
    }
}
