package com.assistant.root.context;

import android.content.Context;
import android.util.Log;
import java.util.List;
import com.assistant.root.cache.HybridCommandCache;

/**
 * Main Hybrid System - Instant patterns + AI fallback
 */
public class HybridCommandSystem {
    private static final String TAG = "HybridSystem";

    private Context context;
    private HybridAIGenerator aiGenerator;
    private HybridCommandCache cache;

    public interface SystemCallback {
        void onCommandReady(String command, boolean instant, String source);

        void onError(String error);
    }

    public HybridCommandSystem(Context context) {
        this.context = context;
        this.aiGenerator = new HybridAIGenerator(context);
        this.cache = new HybridCommandCache(context);
    }

    /**
     * MAIN METHOD - Process command with hybrid approach
     */
    public void processCommand(String userInput, SystemCallback callback) {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();

            try {
                Log.d(TAG, "🚀 Starting hybrid command processing for: " + userInput);

                // Special handling for test commands
                if (userInput.toLowerCase().contains("test hybrid")) {
                    Log.d(TAG, "🧪 Test command detected - providing test response");
                    long time = System.currentTimeMillis() - startTime;
                    callback.onCommandReady("echo 'Hybrid system is working!'", true, "Test (" + time + "ms)");
                    return;
                }

                // Step 1: Get context with timeout
                Log.d(TAG, "📱 Getting app context...");
                ContextDetector.AppContext appContext = ContextDetector.getCurrentContext();
                if (appContext == null) {
                    Log.w(TAG, "⚠️ Context detection failed, using fallback");
                    callback.onError("Could not detect app context - using fallback");
                    return;
                }

                Log.d(TAG, "✅ Context detected: " + appContext.appName);

                // Step 2: Check cache
                String cacheKey = appContext.packageName + ":" + userInput.toLowerCase();
                HybridCommandCache.CachedCommand cached = cache.get(cacheKey);

                if (cached != null) {
                    long time = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "✓ CACHE HIT - " + time + "ms");
                    callback.onCommandReady(cached.command, true, "Cache (" + time + "ms)");
                    return;
                }

                // Step 3: Get UI elements
                Log.d(TAG, "🔍 Getting UI elements...");
                List<UIElementParser.UIElement> elements = UIElementParser.getScreenElements();
                Log.d(TAG, "✅ Found " + elements.size() + " UI elements");

                // Step 4: Try pattern matching (INSTANT)
                Log.d(TAG, "🎯 Trying pattern matching...");
                PatternMatcher.MatchResult match = PatternMatcher.tryMatch(userInput, elements, appContext);

                if (match.matched) {
                    long time = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "✓ PATTERN MATCH - " + time + "ms");

                    // Cache for next time
                    cache.put(cacheKey, match.command);

                    callback.onCommandReady(match.command, true, "Pattern (" + time + "ms)");
                    return;
                }

                // Step 5: Fallback to AI (5-10 seconds)
                Log.d(TAG, "✗ No pattern match, using AI...");

                aiGenerator.generateWithContext(userInput, appContext, elements,
                        new HybridAIGenerator.AICallback() {
                            @Override
                            public void onGenerated(String command) {
                                long time = System.currentTimeMillis() - startTime;
                                Log.d(TAG, "✓ AI GENERATED - " + time + "ms");

                                // Cache for next time
                                cache.put(cacheKey, command);

                                callback.onCommandReady(command, false, "AI (" + time + "ms)");
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

    public String getCacheStats() {
        return cache.getStats();
    }

    public void clearCache() {
        cache.clearAll();
    }
}
