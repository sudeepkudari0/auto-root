package com.assistant.root.skills.ai;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.utils.CommandExecutor;

/**
 * AISkill - handles natural language commands using AI to generate shell
 * commands
 * This skill acts as a fallback for complex commands that don't match other
 * skills
 */
public class AISkill implements Skill {

    @Override
    public boolean matches(String normalizedCommand) {
        // This skill matches commands that can be handled by AI/cache
        // It acts as a fallback for commands that don't match other specific skills

        // Match simple "open" commands (these will be handled by cache)
        if (normalizedCommand.matches("^open\\s+\\w+.*")) {
            return true;
        }

        // Match simple action commands
        if (normalizedCommand
                .matches("^(go\\s+back|go\\s+home|press\\s+enter|take\\s+screenshot|volume\\s+up|volume\\s+down).*")) {
            return true;
        }

        // Match commands that seem to require multiple actions
        if (normalizedCommand.contains(" and ") ||
                normalizedCommand.contains(" then ") ||
                normalizedCommand.contains(" after ") ||
                normalizedCommand.contains(" wait ") ||
                normalizedCommand.contains(" delay ")) {
            return true;
        }

        // Match commands that involve complex interactions
        if (normalizedCommand.contains("send ") && normalizedCommand.contains(" to ") ||
                normalizedCommand.contains("search for ") ||
                normalizedCommand.contains("type ") ||
                normalizedCommand.contains("tap ") ||
                normalizedCommand.contains("press ") ||
                normalizedCommand.contains("scroll ") ||
                normalizedCommand.contains("swipe ")) {
            return true;
        }

        // Match commands that involve URLs or web actions
        if (normalizedCommand.contains("open ") &&
                (normalizedCommand.contains("http") || normalizedCommand.contains("www.") ||
                        normalizedCommand.contains("youtube.com") || normalizedCommand.contains("google.com"))) {
            return true;
        }

        // Match commands that involve specific app actions with additional context
        if (normalizedCommand.contains("whatsapp") &&
                (normalizedCommand.contains("send") || normalizedCommand.contains("message")) &&
                (normalizedCommand.contains(" to ") || normalizedCommand.contains(" contact ") ||
                        normalizedCommand.contains(" number ") || normalizedCommand.contains(" +"))) {
            return true;
        }

        if (normalizedCommand.contains("youtube") &&
                (normalizedCommand.contains("search") || normalizedCommand.contains("play") ||
                        normalizedCommand.contains("trending"))) {
            return true;
        }

        // Match commands that seem to be complex automation tasks
        if (normalizedCommand.length() > 30 &&
                (normalizedCommand.contains("open") || normalizedCommand.contains("launch") ||
                        normalizedCommand.contains("start"))) {
            return true;
        }

        // Match commands with specific contact/phone number patterns
        if (normalizedCommand.contains("+") || normalizedCommand.contains("contact") ||
                normalizedCommand.contains("number") || normalizedCommand.contains("phone")) {
            return true;
        }

        // Match commands that mention specific people by name
        if (normalizedCommand.matches(".*\\b[A-Z][a-z]+\\b.*") &&
                (normalizedCommand.contains("send") || normalizedCommand.contains("message"))) {
            return true;
        }

        return false;
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        executor.log("🤖 AI Skill: Processing complex command - " + normalizedCommand);
        executor.log("🧠 Asking AI to generate commands...");

        // Use AI to generate and execute the command
        executor.executeAICommand(normalizedCommand);
    }
}
