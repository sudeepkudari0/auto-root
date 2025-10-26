package com.assistant.root.skills.base;

import java.util.ArrayList;
import java.util.List;

import com.assistant.root.ui.activities.MainActivity;
import com.assistant.root.utils.CommandExecutor;

/**
 * SkillRegistry - holds a list of skills and dispatches commands to them
 */
public class SkillRegistry {
    private final List<Skill> skills = new ArrayList<>();

    public void register(Skill s) {
        skills.add(s);
    }

    /**
     * Execute the first matching skill. Returns true if handled.
     */
    public boolean execute(String normalizedCommand, CommandExecutor executor) {
        for (Skill s : skills) {
            try {
                if (s.matches(normalizedCommand)) {
                    String skillName = s.getClass().getSimpleName();
                    executor.log("🎯 Using skill: " + skillName);

                    // Update overlay with skill name
                    if (skillName.equals("AISkill")) {
                        // AI skill will handle its own overlay updates
                    } else {
                        executor.updateOverlay("🎯 " + skillName.replace("Skill", ""));
                    }

                    s.execute(normalizedCommand, executor);
                    return true;
                }
            } catch (Exception e) {
                // Skills should not crash the executor; log and continue
                String skillName = s.getClass().getSimpleName();
                executor.log("❌ Skill error in " + skillName + ": " + e.getMessage());
            }
        }
        return false;
    }
}
