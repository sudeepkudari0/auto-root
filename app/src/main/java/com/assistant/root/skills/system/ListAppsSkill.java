package com.assistant.root.skills.system;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.List;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * ListAppsSkill - lists installed apps when user says "list apps" or "show
 * apps"
 */
public class ListAppsSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        return normalizedCommand.contains("list apps") ||
                normalizedCommand.contains("show apps") ||
                normalizedCommand.contains("what apps") ||
                normalizedCommand.contains("available apps");
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        try {
            PackageManager pm = executor.getContext().getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            executor.log("📱 Installed Apps:");
            executor.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            int count = 0;
            for (ApplicationInfo app : packages) {
                // Skip system apps
                if (app.packageName.equals("android") ||
                        app.packageName.startsWith("com.android.") ||
                        app.packageName.startsWith("android.")) {
                    continue;
                }

                String label = pm.getApplicationLabel(app).toString();
                executor.log("  " + (++count) + ". " + label);

                // Limit to 20 apps to avoid spam
                if (count >= 20) {
                    executor.log("  ... and " + (packages.size() - count) + " more apps");
                    break;
                }
            }

            executor.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            executor.log("💡 Say 'open [app name]' to launch any app");

        } catch (Exception e) {
            executor.log("Error listing apps: " + e.getMessage());
        }
    }
}
