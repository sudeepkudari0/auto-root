package com.assistant.root;

/**
 * Skill - small interface for voice command skills
 */
public interface Skill {
    /**
     * Return true if this skill should handle the given normalized command text.
     */
    boolean matches(String normalizedCommand);

    /**
     * Execute the skill. The skill receives the raw command text and the executor
     * to perform actions (open apps, run root commands, etc.).
     */
    void execute(String normalizedCommand, CommandExecutor executor);
}
