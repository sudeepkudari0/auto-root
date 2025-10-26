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
        // This skill matches ONLY complex commands that require AI processing
        // Simple commands should be handled by other skills or cache

        // Match complex "open" commands that have additional actions
        if (normalizedCommand.matches("^open\\s+.*") &&
                (normalizedCommand.contains(" and ") ||
                        normalizedCommand.contains(" then ") ||
                        normalizedCommand.contains(" send ") ||
                        normalizedCommand.contains(" message ") ||
                        normalizedCommand.contains(" to ") ||
                        normalizedCommand.contains(" saying ") ||
                        normalizedCommand.contains(" search ") ||
                        normalizedCommand.contains(" type ") ||
                        normalizedCommand.contains(" tap ") ||
                        normalizedCommand.contains(" press "))) {
            return true;
        }

        // DON'T match simple action commands - let other skills handle them
        // if (normalizedCommand
        // .matches("^(go\\s+back|go\\s+home|press\\s+enter|take\\s+screenshot|volume\\s+up|volume\\s+down).*"))
        // {
        // return true;
        // }

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
                normalizedCommand.contains("swipe ") ||
                normalizedCommand.contains("select ")) {
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

        // Match test commands
        if (normalizedCommand.contains("test context") || normalizedCommand.contains("debug context")) {
            return true;
        }

        // Match hybrid system test commands
        if (normalizedCommand.contains("test hybrid") || normalizedCommand.contains("hybrid test")) {
            return true;
        }

        // Match simple test commands
        if (normalizedCommand.contains("test system") || normalizedCommand.contains("system test")) {
            return true;
        }

        // Match dumpsys test commands
        if (normalizedCommand.contains("test dumpsys") || normalizedCommand.contains("debug dumpsys")) {
            return true;
        }

        return false;
    }

    @Override
    public void execute(String normalizedCommand, CommandExecutor executor) {
        executor.log("🤖 AI Skill: Processing complex command - " + normalizedCommand);

        // Handle test commands
        if (normalizedCommand.contains("test context") || normalizedCommand.contains("debug context")) {
            executor.log("🧪 Testing context detection...");
            executor.testContextDetection();
            return;
        }

        // Handle hybrid system test commands
        if (normalizedCommand.contains("test hybrid") || normalizedCommand.contains("hybrid test")) {
            executor.log("🚀 Testing hybrid system...");
            executor.executeHybridCommand(normalizedCommand);
            return;
        }

        // Handle simple system test commands
        if (normalizedCommand.contains("test system") || normalizedCommand.contains("system test")) {
            executor.log("🧪 Testing system...");
            executor.log("✅ System is working! AI Skill is responding correctly.");
            executor.executeRoot("echo 'System test successful!'");
            return;
        }

        // Handle dumpsys test commands
        if (normalizedCommand.contains("test dump") || normalizedCommand.contains("debug dumpsys")) {
            executor.log("🔍 Testing dumpsys commands...");
            executor.executeRoot("dumpsys window windows | head -20");
            executor.executeRoot("dumpsys activity activities | head -10");
            return;
        }

        // Check if command involves UI interaction first (fast check)
        boolean isUICommand = normalizedCommand.contains("select ") ||
                normalizedCommand.contains("tap ") ||
                normalizedCommand.contains("click ") ||
                normalizedCommand.contains("press ") ||
                normalizedCommand.contains("type ") ||
                normalizedCommand.contains("search ") ||
                normalizedCommand.contains("find ") ||
                normalizedCommand.contains("message ") ||
                normalizedCommand.contains("send ") ||
                normalizedCommand.contains("open ") ||
                normalizedCommand.matches(".*\\b[A-Z][a-z]+\\b.*"); // Contains names

        if (isUICommand) {
            // For UI commands, check context asynchronously
            new Thread(() -> {
                try {
                    String contextInfo = executor.getCurrentContextInfo();
                    executor.log("📱 Context for AI: " + contextInfo);

                    // Check if we're in any app (not home screen)
                    boolean isInApp = !contextInfo.contains("Unknown") && !contextInfo.contains("home screen");

                    if (isInApp) {
                        executor.log("🧠 Detected app context + UI command - using context-aware AI");
                        executor.executeContextAwareCommand(normalizedCommand);
                    } else {
                        executor.log("🧠 No app context detected - using regular AI");
                        executor.executeAICommand(normalizedCommand);
                    }
                } catch (Exception e) {
                    executor.log("⚠️ Error checking context, using regular AI: " + e.getMessage());
                    executor.executeAICommand(normalizedCommand);
                }
            }).start();
        } else {
            executor.log("🧠 Non-UI command - using regular AI to generate commands...");
            executor.executeAICommand(normalizedCommand);
        }
    }
}
