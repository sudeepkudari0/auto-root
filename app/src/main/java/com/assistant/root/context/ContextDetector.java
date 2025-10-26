package com.assistant.root.context;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * Detects current app context using root commands
 * Provides information about what app/screen user is currently viewing
 */
public class ContextDetector {
    private static final String TAG = "ContextDetector";

    public static class AppContext {
        public String packageName;
        public String activityName;
        public String appName;
        public String screenDescription;

        @Override
        public String toString() {
            return "App: " + appName + " (" + packageName + ")\n" +
                    "Activity: " + activityName + "\n" +
                    "Screen: " + screenDescription;
        }
    }

    /**
     * Get current app context using root commands with timeout
     */
    public static AppContext getCurrentContext() {
        AppContext context = new AppContext();

        try {
            Log.d(TAG, "Starting context detection...");
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            Log.d(TAG, "Executing dumpsys command...");
            os.writeBytes("dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp|mInputMethodTarget'\n");
            os.flush();

            // Add timeout protection
            long startTime = System.currentTimeMillis();
            String line = null;
            StringBuilder allOutput = new StringBuilder();

            // Read with timeout
            while ((System.currentTimeMillis() - startTime) < 3000) {
                try {
                    if (reader.ready()) {
                        String currentLine = reader.readLine();
                        if (currentLine != null) {
                            allOutput.append(currentLine).append("\n");
                            Log.d(TAG, "Raw line: " + currentLine);

                            // Look for focus information in various formats
                            if (currentLine.contains("mCurrentFocus") ||
                                    currentLine.contains("mFocusedApp") ||
                                    currentLine.contains("mInputMethodTarget") ||
                                    currentLine.contains("/")) {
                                line = currentLine;
                                break;
                            }
                        }
                    }
                    Thread.sleep(100); // Wait 100ms before checking again
                } catch (InterruptedException e) {
                    break;
                }
            }

            Log.d(TAG, "Final line: " + line);
            Log.d(TAG, "All output: " + allOutput.toString());
            if (line != null && line.contains("/")) {
                String[] parts = line.split(" ");
                for (String part : parts) {
                    if (part.contains("/")) {
                        String[] appParts = part.split("/");
                        context.packageName = appParts[0].trim();
                        context.activityName = appParts[1].replace("}", "").trim();
                        Log.d(TAG, "Parsed package: " + context.packageName + ", activity: " + context.activityName);
                        break;
                    }
                }
            }

            os.writeBytes("exit\n");
            os.flush();

            // Wait for process with timeout
            boolean finished = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                Log.w(TAG, "Process didn't finish in time, destroying");
                process.destroy();
            }

            reader.close();
            os.close();

            if (context.packageName != null) {
                context.appName = getAppName(context.packageName);
                context.screenDescription = getScreenDescription(context.packageName, context.activityName);
                Log.d(TAG, "Detected context: " + context.toString());
            } else {
                Log.w(TAG, "No package name detected, trying alternative method...");
                // Try alternative method
                context = getCurrentContextAlternative();
            }

            return context;

        } catch (Exception e) {
            Log.e(TAG, "Failed to get context: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Alternative method to get current context using different commands
     */
    private static AppContext getCurrentContextAlternative() {
        AppContext context = new AppContext();

        try {
            Log.d(TAG, "Trying alternative context detection...");
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Try different commands
            String[] commands = {
                    "dumpsys activity activities | grep -E 'mResumedActivity|mFocusedActivity'",
                    "dumpsys activity top | grep -E 'ACTIVITY|TASK'",
                    "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'"
            };

            for (String cmd : commands) {
                Log.d(TAG, "Trying command: " + cmd);
                os.writeBytes(cmd + "\n");
                os.flush();

                long startTime = System.currentTimeMillis();
                while ((System.currentTimeMillis() - startTime) < 2000) {
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if (line != null) {
                            Log.d(TAG, "Alt line: " + line);
                            if (line.contains("/")) {
                                String[] parts = line.split(" ");
                                for (String part : parts) {
                                    if (part.contains("/")) {
                                        String[] appParts = part.split("/");
                                        if (appParts.length >= 2) {
                                            context.packageName = appParts[0].trim();
                                            context.activityName = appParts[1].replace("}", "").trim();
                                            Log.d(TAG,
                                                    "Alt parsed: " + context.packageName + "/" + context.activityName);
                                            break;
                                        }
                                    }
                                }
                                if (context.packageName != null)
                                    break;
                            }
                        }
                    }
                    Thread.sleep(100);
                }
                if (context.packageName != null)
                    break;
            }

            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            reader.close();
            os.close();

            if (context.packageName != null) {
                context.appName = getAppName(context.packageName);
                context.screenDescription = getScreenDescription(context.packageName, context.activityName);
                Log.d(TAG, "Alternative context detected: " + context.toString());
            } else {
                Log.w(TAG, "Alternative method also failed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Alternative context detection failed: " + e.getMessage());
        }

        return context;
    }

    /**
     * Get friendly app name from package name
     */
    private static String getAppName(String packageName) {
        switch (packageName) {
            case "com.whatsapp":
                return "WhatsApp";
            case "com.whatsapp.w4b":
                return "WhatsApp Business";
            case "com.google.android.youtube":
                return "YouTube";
            case "com.instagram.android":
                return "Instagram";
            case "com.facebook.katana":
                return "Facebook";
            case "com.twitter.android":
                return "Twitter";
            case "org.telegram.messenger":
                return "Telegram";
            case "com.google.android.apps.maps":
                return "Google Maps";
            case "com.android.chrome":
                return "Chrome";
            case "com.google.android.gm":
                return "Gmail";
            case "com.spotify.music":
                return "Spotify";
            case "com.netflix.mediaclient":
                return "Netflix";
            case "com.amazon.mShop.android.shopping":
                return "Amazon";
            default:
                return packageName;
        }
    }

    /**
     * Get screen description from activity name
     */
    private static String getScreenDescription(String packageName, String activityName) {
        if (activityName == null)
            return "Unknown Screen";

        // WhatsApp screens
        if (packageName.equals("com.whatsapp")) {
            if (activityName.contains("HomeActivity"))
                return "Chats List";
            if (activityName.contains("Conversation"))
                return "Chat Conversation";
            if (activityName.contains("ContactPicker"))
                return "Contact Selection";
            if (activityName.contains("Status"))
                return "Status Screen";
            if (activityName.contains("Call"))
                return "Call Screen";
        }

        // YouTube screens
        if (packageName.equals("com.google.android.youtube")) {
            if (activityName.contains("WatchWhileActivity"))
                return "Video Player";
            if (activityName.contains("HomeActivity"))
                return "Home Feed";
            if (activityName.contains("SearchActivity"))
                return "Search Screen";
            if (activityName.contains("SubscriptionFeed"))
                return "Subscriptions";
        }

        // Google Maps screens
        if (packageName.equals("com.google.android.apps.maps")) {
            if (activityName.contains("MapsActivity"))
                return "Map View";
            if (activityName.contains("SearchActivity"))
                return "Search Screen";
            if (activityName.contains("NavigationActivity"))
                return "Navigation";
        }

        // Instagram screens
        if (packageName.equals("com.instagram.android")) {
            if (activityName.contains("MainTabActivity"))
                return "Home Feed";
            if (activityName.contains("DirectInboxActivity"))
                return "Direct Messages";
            if (activityName.contains("CameraActivity"))
                return "Camera/Story";
        }

        // Chrome screens
        if (packageName.equals("com.android.chrome")) {
            if (activityName.contains("ChromeTabbedActivity"))
                return "Browser Tab";
            if (activityName.contains("Incognito"))
                return "Incognito Mode";
        }

        return "Main Screen";
    }

    /**
     * Check if user is in a specific app
     */
    public static boolean isInApp(String packageName) {
        AppContext context = getCurrentContext();
        return context != null && context.packageName.equals(packageName);
    }

    /**
     * Get simplified context for AI
     */
    public static String getContextSummary() {
        AppContext context = getCurrentContext();
        if (context == null) {
            return "Context: Unknown (device home screen or locked)";
        }

        return String.format(
                "Current App: %s\n" +
                        "Current Screen: %s\n" +
                        "Package: %s\n" +
                        "Activity: %s",
                context.appName,
                context.screenDescription,
                context.packageName,
                context.activityName);
    }
}
