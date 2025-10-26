package com.assistant.root.skills.system;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * ToggleWiFiSkill - toggles Wi-Fi on/off when commanded.
 */
public class ToggleWiFiSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        // Only match simple WiFi commands without additional actions
        if (!normalizedCommand.contains("wifi") ||
                (!normalizedCommand.contains("toggle") && !normalizedCommand.contains("turn") &&
                        !normalizedCommand.contains("enable") && !normalizedCommand.contains("disable") &&
                        !normalizedCommand.contains("on") && !normalizedCommand.contains("off"))) {
            return false;
        }

        // Check if command contains additional actions that should be handled by AI
        String[] additionalActionKeywords = {
                " and ", " then ", " after ", " wait ", " delay ",
                " send ", " message ", " type ", " tap ", " press ",
                " search ", " find ", " go to ", " click ", " scroll ",
                " swipe ", " open ", " close ", " switch "
        };

        for (String keyword : additionalActionKeywords) {
            if (normalizedCommand.contains(keyword)) {
                // This command has additional actions, let AI handle it
                return false;
            }
        }

        // Check if command is too long (likely complex)
        if (normalizedCommand.length() > 25) {
            return false;
        }

        return true;
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        boolean enable = normalizedCommand.contains("on") || normalizedCommand.contains("enable");
        // If it explicitly says off/disable, honor that
        if (normalizedCommand.contains("off") || normalizedCommand.contains("disable"))
            enable = false;

        executor.toggleWiFi(enable);
        executor.log("Toggle WiFi -> " + (enable ? "enable" : "disable"));
    }
}
