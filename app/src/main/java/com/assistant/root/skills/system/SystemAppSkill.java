package com.assistant.root.skills.system;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * SystemAppSkill - uses system-level commands to open apps, bypassing Android
 * restrictions
 */
public class SystemAppSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        return normalizedCommand.contains("system open ") ||
                normalizedCommand.contains("force open ");
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        // Extract app name
        String appName = normalizedCommand.replaceAll("(system open|force open)\\s+", "").trim();

        if (appName.isEmpty()) {
            executor.log("Could not determine which app to force open");
            return;
        }

        executor.log("🔧 Force opening: " + appName);

        // Use multiple aggressive root methods
        try {
            // Method 1: Kill any existing instance and restart
            executor.executeRoot("am force-stop " + appName);
            Thread.sleep(200);

            // Method 2: Use monkey with multiple attempts
            executor.executeRoot("monkey -p " + appName + " -c android.intent.category.LAUNCHER 1");
            Thread.sleep(500);

            // Method 3: Use am start with system privileges
            executor.executeRoot(
                    "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10000000 "
                            + appName);
            Thread.sleep(500);

            // Method 4: Try with different intent flags
            executor.executeRoot(
                    "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x20000000 "
                            + appName);

            executor.log("Force open attempted for: " + appName);

        } catch (Exception e) {
            executor.log("Force open error: " + e.getMessage());
        }
    }
}
