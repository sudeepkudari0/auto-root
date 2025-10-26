package com.assistant.root.context;

import android.util.Log;

/**
 * App-specific context handlers for common actions
 * Provides quick templates for common tasks in specific apps
 */
public class AppContextHandler {
    private static final String TAG = "AppContextHandler";

    /**
     * Get quick command template for specific app and action
     */
    public static String getQuickCommand(String packageName, String userInput) {
        String lowerInput = userInput.toLowerCase();

        // WhatsApp quick commands
        if (packageName.equals("com.whatsapp")) {
            return getWhatsAppQuickCommand(lowerInput);
        }

        // YouTube quick commands
        if (packageName.equals("com.google.android.youtube")) {
            return getYouTubeQuickCommand(lowerInput);
        }

        // Google Maps quick commands
        if (packageName.equals("com.google.android.apps.maps")) {
            return getMapsQuickCommand(lowerInput);
        }

        // Instagram quick commands
        if (packageName.equals("com.instagram.android")) {
            return getInstagramQuickCommand(lowerInput);
        }

        // Chrome quick commands
        if (packageName.equals("com.android.chrome")) {
            return getChromeQuickCommand(lowerInput);
        }

        // Gmail quick commands
        if (packageName.equals("com.google.android.gm")) {
            return getGmailQuickCommand(lowerInput);
        }

        // Settings quick commands
        if (packageName.equals("com.android.settings")) {
            return getSettingsQuickCommand(lowerInput);
        }

        // Camera quick commands
        if (packageName.equals("com.android.camera") || packageName.equals("com.google.android.GoogleCamera")) {
            return getCameraQuickCommand(lowerInput);
        }

        // Gallery/Photos quick commands
        if (packageName.equals("com.google.android.apps.photos") || packageName.equals("com.android.gallery3d")) {
            return getGalleryQuickCommand(lowerInput);
        }

        return null; // No quick template available
    }

    /**
     * WhatsApp quick commands
     */
    private static String getWhatsAppQuickCommand(String userInput) {
        // Search for contact
        if (userInput.contains("search") || userInput.contains("find")) {
            return "input tap 950 150\n" + // Search button
                    "sleep 1";
        }

        // Send message to contact
        if (userInput.contains("message") || userInput.contains("send")) {
            return "input tap 950 150\n" + // Search button
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back") || userInput.contains("return")) {
            return "input keyevent 4";
        }

        // Open status
        if (userInput.contains("status")) {
            return "input tap 950 200\n" + // Status tab
                    "sleep 1";
        }

        // Open calls
        if (userInput.contains("call") || userInput.contains("calls")) {
            return "input tap 950 250\n" + // Calls tab
                    "sleep 1";
        }

        return null;
    }

    /**
     * YouTube quick commands
     */
    private static String getYouTubeQuickCommand(String userInput) {
        // Search
        if (userInput.contains("search")) {
            return "input tap 540 200\n" + // Search button
                    "sleep 1";
        }

        // Go to home
        if (userInput.contains("home") || userInput.contains("trending")) {
            return "input tap 200 200\n" + // Home tab
                    "sleep 1";
        }

        // Go to subscriptions
        if (userInput.contains("subscription") || userInput.contains("subscribed")) {
            return "input tap 400 200\n" + // Subscriptions tab
                    "sleep 1";
        }

        // Go to library
        if (userInput.contains("library") || userInput.contains("playlist")) {
            return "input tap 600 200\n" + // Library tab
                    "sleep 1";
        }

        // Play/pause
        if (userInput.contains("play") || userInput.contains("pause")) {
            return "input tap 540 1000\n" + // Center of video
                    "sleep 0.5";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        return null;
    }

    /**
     * Google Maps quick commands
     */
    private static String getMapsQuickCommand(String userInput) {
        // Search
        if (userInput.contains("search")) {
            return "input tap 540 200\n" + // Search box
                    "sleep 1";
        }

        // Get directions
        if (userInput.contains("direction") || userInput.contains("navigate")) {
            return "input tap 950 200\n" + // Directions button
                    "sleep 1";
        }

        // My location
        if (userInput.contains("location") || userInput.contains("where am i")) {
            return "input tap 100 1000\n" + // My location button
                    "sleep 1";
        }

        // Layers
        if (userInput.contains("layer") || userInput.contains("satellite")) {
            return "input tap 100 200\n" + // Layers button
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        return null;
    }

    /**
     * Instagram quick commands
     */
    private static String getInstagramQuickCommand(String userInput) {
        // Search
        if (userInput.contains("search")) {
            return "input tap 950 200\n" + // Search tab
                    "sleep 1";
        }

        // Messages/DM
        if (userInput.contains("message") || userInput.contains("dm") || userInput.contains("direct")) {
            return "input tap 950 250\n" + // Messages tab
                    "sleep 1";
        }

        // Camera/Story
        if (userInput.contains("camera") || userInput.contains("story")) {
            return "input tap 540 200\n" + // Camera button
                    "sleep 1";
        }

        // Profile
        if (userInput.contains("profile") || userInput.contains("account")) {
            return "input tap 100 200\n" + // Profile tab
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        return null;
    }

    /**
     * Chrome quick commands
     */
    private static String getChromeQuickCommand(String userInput) {
        // Search/address bar
        if (userInput.contains("search") || userInput.contains("url") || userInput.contains("address")) {
            return "input tap 540 150\n" + // Address bar
                    "sleep 1";
        }

        // New tab
        if (userInput.contains("new tab") || userInput.contains("new page")) {
            return "input tap 950 150\n" + // New tab button
                    "sleep 1";
        }

        // Bookmarks
        if (userInput.contains("bookmark") || userInput.contains("favorite")) {
            return "input tap 100 150\n" + // Menu button
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        // Go forward
        if (userInput.contains("forward")) {
            return "input tap 200 150\n" + // Forward button
                    "sleep 1";
        }

        return null;
    }

    /**
     * Get app-specific help text
     */
    public static String getAppHelp(String packageName) {
        switch (packageName) {
            case "com.whatsapp":
                return "WhatsApp Commands:\n" +
                        "• 'search [name]' - Search for contact\n" +
                        "• 'message [name]' - Start new message\n" +
                        "• 'status' - Open status tab\n" +
                        "• 'calls' - Open calls tab\n" +
                        "• 'back' - Go back";

            case "com.google.android.youtube":
                return "YouTube Commands:\n" +
                        "• 'search [query]' - Search videos\n" +
                        "• 'home' - Go to home feed\n" +
                        "• 'subscriptions' - View subscriptions\n" +
                        "• 'library' - Open library\n" +
                        "• 'play/pause' - Control video";

            case "com.google.android.apps.maps":
                return "Maps Commands:\n" +
                        "• 'search [place]' - Search location\n" +
                        "• 'directions to [place]' - Get directions\n" +
                        "• 'my location' - Show current location\n" +
                        "• 'layers' - Change map type\n" +
                        "• 'back' - Go back";

            case "com.instagram.android":
                return "Instagram Commands:\n" +
                        "• 'search [user]' - Search users\n" +
                        "• 'messages' - Open DMs\n" +
                        "• 'camera' - Open camera\n" +
                        "• 'profile' - View profile\n" +
                        "• 'back' - Go back";

            case "com.android.chrome":
                return "Chrome Commands:\n" +
                        "• 'search [query]' - Search web\n" +
                        "• 'new tab' - Open new tab\n" +
                        "• 'bookmarks' - View bookmarks\n" +
                        "• 'back/forward' - Navigate\n" +
                        "• 'back' - Go back";

            default:
                return "Available commands depend on the current app.\n" +
                        "Try: 'search', 'back', 'home', or describe what you want to do.";
        }
    }

    /**
     * Gmail quick commands
     */
    private static String getGmailQuickCommand(String userInput) {
        // Compose new email
        if (userInput.contains("compose") || userInput.contains("new email") || userInput.contains("write")) {
            return "input tap 950 200\n" + // Compose button
                    "sleep 1";
        }

        // Search emails
        if (userInput.contains("search")) {
            return "input tap 540 200\n" + // Search button
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        return null;
    }

    /**
     * Settings quick commands
     */
    private static String getSettingsQuickCommand(String userInput) {
        // Search settings
        if (userInput.contains("search")) {
            return "input tap 540 200\n" + // Search button
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        return null;
    }

    /**
     * Camera quick commands
     */
    private static String getCameraQuickCommand(String userInput) {
        // Take photo
        if (userInput.contains("photo") || userInput.contains("capture") || userInput.contains("take")) {
            return "input tap 540 1000\n" + // Capture button
                    "sleep 0.5";
        }

        // Switch to video
        if (userInput.contains("video") || userInput.contains("record")) {
            return "input tap 950 200\n" + // Video mode button
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        return null;
    }

    /**
     * Gallery/Photos quick commands
     */
    private static String getGalleryQuickCommand(String userInput) {
        // Search photos
        if (userInput.contains("search")) {
            return "input tap 540 200\n" + // Search button
                    "sleep 1";
        }

        // Go back
        if (userInput.contains("back")) {
            return "input keyevent 4";
        }

        return null;
    }

    /**
     * Check if app supports quick commands
     */
    public static boolean supportsQuickCommands(String packageName) {
        if (packageName == null || packageName.isEmpty())
            return false;

        return packageName.equals("com.whatsapp") ||
                packageName.equals("com.google.android.youtube") ||
                packageName.equals("com.google.android.apps.maps") ||
                packageName.equals("com.instagram.android") ||
                packageName.equals("com.android.chrome") ||
                packageName.equals("com.google.android.gm") ||
                packageName.equals("com.android.settings") ||
                packageName.equals("com.android.camera") ||
                packageName.equals("com.google.android.GoogleCamera") ||
                packageName.equals("com.google.android.apps.photos") ||
                packageName.equals("com.android.gallery3d");
    }

    /**
     * Get common actions for current app
     */
    public static String[] getCommonActions(String packageName) {
        switch (packageName) {
            case "com.whatsapp":
                return new String[] { "search", "message", "status", "calls", "back" };

            case "com.google.android.youtube":
                return new String[] { "search", "home", "subscriptions", "library", "play" };

            case "com.google.android.apps.maps":
                return new String[] { "search", "directions", "location", "layers", "back" };

            case "com.instagram.android":
                return new String[] { "search", "messages", "camera", "profile", "back" };

            case "com.android.chrome":
                return new String[] { "search", "new tab", "bookmarks", "back", "forward" };

            default:
                return new String[] { "search", "back", "home" };
        }
    }
}
