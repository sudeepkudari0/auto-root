package com.assistant.root;

import android.content.Intent;
import android.net.Uri;

/**
 * UrlOpenSkill - opens apps using URL schemes (like WhatsApp method)
 * This bypasses Android's app interaction blocking
 */
public class UrlOpenSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        return normalizedCommand.contains("url open ") ||
                normalizedCommand.contains("web open ");
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        // Extract app name
        String appName = normalizedCommand.replaceAll("(url open|web open)\\s+", "").trim();

        if (appName.isEmpty()) {
            executor.log("Could not determine which app to open via URL");
            return;
        }

        executor.log("🌐 URL opening: " + appName);

        // Use URL-based approach like WhatsApp
        try {
            // Try different URL schemes
            String[] urlSchemes = {
                    "https://" + appName + ".com", // Website
                    "https://play.google.com/store/apps/details?id=" + appName, // Play Store
                    "market://details?id=" + appName, // Play Store app
                    appName + "://", // App scheme
                    "intent://" + appName // Intent scheme
            };

            for (String url : urlSchemes) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    executor.getContext().startActivity(intent);
                    executor.log("Opened " + appName + " using URL: " + url);
                    return;
                } catch (Exception e) {
                    // Try next URL
                }
            }

            executor.log("All URL methods failed for: " + appName);

        } catch (Exception e) {
            executor.log("URL open error: " + e.getMessage());
        }
    }
}
