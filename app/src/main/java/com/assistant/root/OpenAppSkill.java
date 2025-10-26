package com.assistant.root;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.List;

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
        return normalizedCommand.contains("open ") || normalizedCommand.contains("launch ");
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

        // Find and open the app
        String packageName = findAppPackage(executor.getContext(), appName);
        if (packageName != null) {
            executor.openApp(packageName);
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

    private String findAppPackage(Context context, String appName) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String appNameLower = appName.toLowerCase();

        // First pass: exact matches
        for (ApplicationInfo app : packages) {
            if (app.packageName.equals("android") || app.packageName.startsWith("com.android.")) {
                continue; // Skip system apps
            }

            String label = pm.getApplicationLabel(app).toString().toLowerCase();
            String packageName = app.packageName.toLowerCase();

            // Exact name match
            if (label.equals(appNameLower) || packageName.contains(appNameLower)) {
                return app.packageName;
            }
        }

        // Second pass: contains matches
        for (ApplicationInfo app : packages) {
            if (app.packageName.equals("android") || app.packageName.startsWith("com.android.")) {
                continue;
            }

            String label = pm.getApplicationLabel(app).toString().toLowerCase();
            String packageName = app.packageName.toLowerCase();

            if (label.contains(appNameLower) || packageName.contains(appNameLower)) {
                return app.packageName;
            }
        }

        // Third pass: fuzzy matching with common app names
        for (String commonName : COMMON_APP_NAMES) {
            if (appNameLower.contains(commonName)) {
                for (ApplicationInfo app : packages) {
                    if (app.packageName.equals("android") || app.packageName.startsWith("com.android.")) {
                        continue;
                    }

                    String label = pm.getApplicationLabel(app).toString().toLowerCase();
                    String packageName = app.packageName.toLowerCase();

                    if (label.contains(commonName) || packageName.contains(commonName)) {
                        return app.packageName;
                    }
                }
            }
        }

        return null;
    }

    private void listSomeApps(CommandExecutor executor) {
        try {
            PackageManager pm = executor.getContext().getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            executor.log("Some available apps:");
            int count = 0;
            for (ApplicationInfo app : packages) {
                if (app.packageName.equals("android") || app.packageName.startsWith("com.android.")) {
                    continue;
                }

                String label = pm.getApplicationLabel(app).toString();
                executor.log("  - " + label);

                if (++count >= 10)
                    break; // Show only first 10 apps
            }
        } catch (Exception e) {
            executor.log("Error listing apps: " + e.getMessage());
        }
    }
}
