package com.assistant.root.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Smart caching system that learns from AI responses
 * Eliminates delay for frequently used commands
 */
public class HybridCommandCache {
    private static final String TAG = "CommandCache";
    private static final String PREFS_NAME = "command_cache";
    private static final String CACHE_KEY = "cached_commands";

    private SharedPreferences prefs;
    private Gson gson;
    private Map<String, CachedCommand> cache;

    public static class CachedCommand {
        public String command;
        public long timestamp;
        public int useCount;

        public CachedCommand(String command) {
            this.command = command;
            this.timestamp = System.currentTimeMillis();
            this.useCount = 1;
        }
    }

    public HybridCommandCache(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        loadCache();
        initializeCommonCommands();
        Log.d(TAG, "Cache initialized with " + cache.size() + " commands");
    }

    /**
     * Load cache from storage
     */
    private void loadCache() {
        String json = prefs.getString(CACHE_KEY, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, CachedCommand>>() {
            }.getType();
            cache = gson.fromJson(json, type);
        } else {
            cache = new HashMap<>();
        }
        Log.d(TAG, "Loaded " + cache.size() + " cached commands");
    }

    /**
     * Pre-populate with common commands
     */
    private void initializeCommonCommands() {
        if (cache.isEmpty()) {
            // WhatsApp
            put("open whatsapp", "am start -n com.whatsapp/.HomeActivity");
            put("open WhatsApp", "am start -n com.whatsapp/.HomeActivity");
            put("open WHATSAPP", "am start -n com.whatsapp/.HomeActivity");
            put("open whatsapp business", "am start -n com.whatsapp.w4b/.HomeActivity");

            // YouTube
            put("open youtube", "am start -n com.google.android.youtube/.HomeActivity");
            put("open YouTube", "am start -n com.google.android.youtube/.HomeActivity");
            put("open YOUTUBE", "am start -n com.google.android.youtube/.HomeActivity");
            put("open youtube trending",
                    "am start -a android.intent.action.VIEW -d 'vnd.youtube://www.youtube.com/feed/trending'");
            put("open youtube shorts",
                    "am start -a android.intent.action.VIEW -d 'vnd.youtube://www.youtube.com/shorts'");

            // Social Media
            put("open instagram", "am start -n com.instagram.android/.activity.MainTabActivity");
            put("open facebook", "am start -n com.facebook.katana/.activity.FbMainTabActivity");
            put("open twitter", "am start -n com.twitter.android/.StartActivity");
            put("open telegram", "am start -n org.telegram.messenger/.DefaultIcon");

            // Google Apps
            put("open gmail", "am start -n com.google.android.gm/.ConversationListActivityGmail");
            put("open chrome", "am start -n com.android.chrome/com.google.android.apps.chrome.Main");
            put("open maps", "am start -n com.google.android.apps.maps/com.google.android.maps.MapsActivity");
            put("open drive", "am start -n com.google.android.apps.docs/.app.NewMainProxyActivity");
            put("open photos", "am start -n com.google.android.apps.photos/.home.HomeActivity");

            // System Apps
            put("open settings", "am start -n com.android.settings/.Settings");
            put("open camera", "am start -n com.android.camera/.Camera");
            put("open gallery", "am start -a android.intent.action.VIEW -t 'image/*'");

            // Common Actions
            put("go back", "input keyevent 4");
            put("go home", "input keyevent 3");
            put("press enter", "input keyevent 66");
            put("volume up", "input keyevent 24");
            put("volume down", "input keyevent 25");
            put("take screenshot", "input keyevent 120");

            saveCache();
            Log.d(TAG, "Initialized with " + cache.size() + " common commands");
        }
    }

    /**
     * Check if command is cached
     * Uses fuzzy matching for better hits
     */
    public CachedCommand get(String userInput) {
        String normalized = normalizeInput(userInput);
        Log.d(TAG, "Looking for: '" + userInput + "' -> normalized: '" + normalized + "'");
        Log.d(TAG, "Cache contains " + cache.size() + " entries");

        // Exact match
        if (cache.containsKey(normalized)) {
            CachedCommand cmd = cache.get(normalized);
            cmd.useCount++;
            saveCache();
            Log.d(TAG, "Cache HIT: " + normalized);
            return cmd;
        }

        // Fuzzy match (contains keywords)
        for (Map.Entry<String, CachedCommand> entry : cache.entrySet()) {
            if (isSimilar(normalized, entry.getKey())) {
                CachedCommand cmd = entry.getValue();
                cmd.useCount++;
                saveCache();
                Log.d(TAG, "Cache FUZZY HIT: " + entry.getKey());
                return cmd;
            }
        }

        Log.d(TAG, "Cache MISS: " + normalized);
        Log.d(TAG, "Available cache keys: " + cache.keySet());
        return null;
    }

    /**
     * Store new command from AI
     */
    public void put(String userInput, String command) {
        String normalized = normalizeInput(userInput);

        if (cache.containsKey(normalized)) {
            cache.get(normalized).useCount++;
        } else {
            cache.put(normalized, new CachedCommand(command));
            Log.d(TAG, "Cached new command: " + normalized);
        }

        saveCache();
    }

    /**
     * Save cache to storage
     */
    private void saveCache() {
        String json = gson.toJson(cache);
        prefs.edit().putString(CACHE_KEY, json).apply();
    }

    /**
     * Normalize user input for better matching
     */
    private String normalizeInput(String input) {
        return input.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9\\s]", "");
    }

    /**
     * Check if two inputs are similar enough
     * Only match if they are very similar (not just containing same words)
     */
    private boolean isSimilar(String input1, String input2) {
        String[] words1 = input1.split(" ");
        String[] words2 = input2.split(" ");

        // If lengths are very different, they're not similar
        if (Math.abs(words1.length - words2.length) > 1) {
            return false;
        }

        // If one is much longer than the other, they're not similar
        if (words1.length > words2.length * 2 || words2.length > words1.length * 2) {
            return false;
        }

        int matches = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    matches++;
                    break;
                }
            }
        }

        // Require at least 80% word match AND similar length
        double matchRatio = (double) matches / Math.min(words1.length, words2.length);
        return matchRatio >= 0.8 && Math.abs(words1.length - words2.length) <= 1;
    }

    /**
     * Clear old/unused cache entries
     */
    public void cleanup() {
        long oneMonthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        cache.entrySet().removeIf(entry -> entry.getValue().useCount < 2 &&
                entry.getValue().timestamp < oneMonthAgo);

        saveCache();
        Log.d(TAG, "Cleaned cache, " + cache.size() + " entries remaining");
    }

    /**
     * Get cache statistics
     */
    public String getStats() {
        int totalUses = 0;
        for (CachedCommand cmd : cache.values()) {
            totalUses += cmd.useCount;
        }

        return "Cached Commands: " + cache.size() + "\n" +
                "Total Uses: " + totalUses + "\n" +
                "Avg Uses/Command: " + (cache.isEmpty() ? 0 : totalUses / cache.size());
    }

    /**
     * Clear all cache
     */
    public void clearAll() {
        cache.clear();
        saveCache();
        initializeCommonCommands();
    }
}
