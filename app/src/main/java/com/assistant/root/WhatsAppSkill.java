package com.assistant.root;

/**
 * WhatsAppSkill - handles simple "send whatsapp" commands.
 * Current parsing is naive and expects a number or name followed by "message".
 */
public class WhatsAppSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        return normalizedCommand.contains("whatsapp") && (normalizedCommand.contains("send") || normalizedCommand.contains("message"));
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
                if (idx >= 0) who = body.substring(idx + 2).trim();
            }

            executor.sendWhatsAppMessage(who, msg);
            executor.log("WhatsAppSkill invoked: to=" + who + " msg=" + msg);
        } catch (Exception e) {
            executor.log("WhatsAppSkill error: " + e.getMessage());
        }
    }
}
