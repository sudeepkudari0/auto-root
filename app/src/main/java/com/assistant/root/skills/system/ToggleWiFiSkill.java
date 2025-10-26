package com.assistant.root.skills.system;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * ToggleWiFiSkill - toggles Wi-Fi on/off when commanded.
 */
public class ToggleWiFiSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        return normalizedCommand.contains("wifi")
                && (normalizedCommand.contains("toggle") || normalizedCommand.contains("turn")
                        || normalizedCommand.contains("enable") || normalizedCommand.contains("disable")
                        || normalizedCommand.contains("on") || normalizedCommand.contains("off"));
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
