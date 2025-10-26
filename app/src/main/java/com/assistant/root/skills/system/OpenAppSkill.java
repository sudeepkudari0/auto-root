package com.assistant.root.skills.system;

import java.util.List;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * OpenAppSkill - opens ANY installed app when user says "open X" or "launch X".
 * Uses fuzzy matching to find apps by name.
 */
public class OpenAppSkill implements Skill {

    private static final String[] COMMON_APP_NAMES = {
            "whatsapp", "telegram", "chrome", "browser", "firefox", "safari", "edge",
            "youtube", "instagram", "facebook", "twitter", "tiktok", "snapchat",
            "gmail", "outlook", "mail", "calendar", "clock", "calculator",
            "camera", "gallery", "photos", "music", "spotify", "netflix",
            "maps", "google maps", "waze", "uber", "settings", "contacts",
            "phone", "messages", "sms", "dialer", "files", "file manager",
            "notes", "notepad", "keep", "evernote", "pdf", "reader",
            "office", "word", "excel", "powerpoint", "adobe", "photoshop"
    };

    @Override
    public boolean matches(String normalizedCommand) {
        // Only match simple "open" commands (2-3 words max)
        // Don't match complex commands like "open whatsapp and send message to x"

        // Check if it starts with "open" or "launch"
        if (!normalizedCommand.matches("^(open|launch)\\s+.*")) {
            return false;
        }

        // Count words in the command
        String[] words = normalizedCommand.trim().split("\\s+");

        // Only match if it's 2-3 words (e.g., "open whatsapp", "open google maps")
        if (words.length > 3) {
            return false;
        }

        // Don't match if it contains complex action words
        String lowerCommand = normalizedCommand.toLowerCase();
        if (lowerCommand.contains(" and ") ||
                lowerCommand.contains(" then ") ||
                lowerCommand.contains(" after ") ||
                lowerCommand.contains(" send ") ||
                lowerCommand.contains(" message ") ||
                lowerCommand.contains(" to ") ||
                lowerCommand.contains(" saying ") ||
                lowerCommand.contains(" with ") ||
                lowerCommand.contains(" search ") ||
                lowerCommand.contains(" type ") ||
                lowerCommand.contains(" tap ") ||
                lowerCommand.contains(" press ")) {
            return false;
        }

        return true;
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        // Extract app name from command
        String appName = extractAppName(normalizedCommand);
        if (appName.isEmpty()) {
            executor.log("Could not determine which app to open");
            return;
        }

        executor.log("Looking for app: \"" + appName + "\"");

        // Find and open the app using su command approach
        String packageName = executor.findAppPackageByName(appName);
        if (packageName != null) {
            executor.openAppWithSu(packageName);
            executor.log("Opening: " + appName + " (" + packageName + ")");
        } else {
            executor.log("App not found: " + appName);
            // Try to list some available apps for user reference
            listSomeApps(executor);
        }
    }

    private String extractAppName(String command) {
        // Remove "open" or "launch" and get the app name
        String appName = command.replaceAll("(open|launch)\\s+", "").trim();

        // Remove common suffixes
        appName = appName.replaceAll("\\s+(app|application)$", "");

        return appName;
    }

    private void listSomeApps(CommandExecutor executor) {
        try {
            List<String> packages = executor.getAllInstalledPackages();

            executor.log("Some available apps:");
            int count = 0;
            for (String packageName : packages) {
                if (packageName.equals("android") || packageName.startsWith("com.android.")) {
                    continue;
                }

                executor.log("  - " + packageName);

                if (++count >= 10)
                    break; // Show only first 10 apps
            }
        } catch (Exception e) {
            executor.log("Error listing apps: " + e.getMessage());
        }
    }
}
