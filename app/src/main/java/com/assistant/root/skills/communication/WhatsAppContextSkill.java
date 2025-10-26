package com.assistant.root.skills.communication;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * WhatsAppContextSkill - handles WhatsApp-specific commands when user is
 * already in WhatsApp
 * Uses context-aware AI to interact with WhatsApp UI elements
 */
public class WhatsAppContextSkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        // Check if we're in WhatsApp first
        // This will be checked in execute() method since we need CommandExecutor
        // instance

        // Match WhatsApp-specific commands
        return normalizedCommand.contains("select ") ||
                normalizedCommand.contains("message ") ||
                normalizedCommand.contains("send ") ||
                normalizedCommand.contains("chat ") ||
                normalizedCommand.contains("contact ") ||
                normalizedCommand.contains("search ") ||
                normalizedCommand.contains("find ") ||
                normalizedCommand.contains("status") ||
                normalizedCommand.contains("calls") ||
                normalizedCommand.contains("back") ||
                // Match commands with names (like "select John", "message Sarah")
                normalizedCommand.matches(".*\\b[A-Z][a-z]+\\b.*");
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        // Check if we're actually in WhatsApp
        String contextInfo = executor.getCurrentContextInfo();
        if (!contextInfo.contains("WhatsApp")) {
            executor.log("⚠️ WhatsApp context skill called but not in WhatsApp - skipping");
            return;
        }

        executor.log("📱 WhatsApp Context Skill: Processing - " + normalizedCommand);
        executor.log("🧠 Using context-aware AI for WhatsApp interaction...");

        // Use context-aware command execution
        executor.executeContextAwareCommand(normalizedCommand);
    }
}
