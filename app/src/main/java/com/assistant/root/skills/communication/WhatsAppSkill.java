package com.assistant.root.skills.communication;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * WhatsAppSkill - handles simple "send whatsapp" commands.
 * Current parsing is naive and expects a number or name followed by "message".
 */
public class WhatsAppSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        // Only match commands that explicitly mention "whatsapp"
        if (!normalizedCommand.contains("whatsapp")) {
            return false;
        }

        // Must also contain send or message
        if (!normalizedCommand.contains("send") && !normalizedCommand.contains("message")) {
            return false;
        }

        // Check if command contains additional actions that should be handled by AI
        String[] additionalActionKeywords = {
                " and ", " then ", " after ", " wait ", " delay ",
                " open ", " close ", " switch ", " turn ", " tap ", " press ",
                " search ", " find ", " go to ", " click ", " scroll ", " swipe "
        };

        for (String keyword : additionalActionKeywords) {
            if (normalizedCommand.contains(keyword)) {
                // This command has additional actions, let AI handle it
                return false;
            }
        }

        // Check if command is too long (likely complex)
        if (normalizedCommand.length() > 50) {
            return false;
        }

        // Check if command contains multiple apps (likely complex)
        String[] otherApps = { "youtube", "chrome", "instagram", "facebook", "twitter", "telegram", "gmail" };
        int appCount = 0;
        for (String app : otherApps) {
            if (normalizedCommand.contains(app)) {
                appCount++;
            }
        }

        // If other apps mentioned, let AI handle it
        if (appCount > 0) {
            return false;
        }

        return true;
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        // Naive grammar: "send whatsapp to <number|name> message <text>"
        try {
            String body = normalizedCommand;
            String who = "";
            String msg = "";

            if (body.contains("message")) {
                String[] parts = body.split("message", 2);
                msg = parts.length > 1 ? parts[1].trim() : "";
                String left = parts[0];
                left = left.replace("send whatsapp to", "").replace("send whatsapp", "").trim();
                who = left;
            } else {
                // fallback: everything after 'to'
                int idx = body.indexOf("to");
                if (idx >= 0)
                    who = body.substring(idx + 2).trim();
            }

            executor.sendWhatsAppMessage(who, msg);
            executor.log("WhatsAppSkill invoked: to=" + who + " msg=" + msg);
        } catch (Exception e) {
            executor.log("WhatsAppSkill error: " + e.getMessage());
        }
    }
}
