package com.assistant.root;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * CommandExecutor - executes parsed commands either via root shell or Android
 * APIs.
 *
 * Add new "skills" by creating a method and calling it from
 * `executeParsedCommand`.
 */
public class CommandExecutor {

    private static final String TAG = "CommandExecutor";

    private final Context context;
    private final SkillRegistry skills;

    public CommandExecutor(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.skills = new SkillRegistry();

        // Register built-in skills. Add more skills by creating classes implementing
        // Skill.
        this.skills.register(new OpenAppSkill());
        this.skills.register(new WhatsAppSkill());
        this.skills.register(new ToggleWiFiSkill());
        this.skills.register(new ListAppsSkill());
        this.skills.register(new SystemAppSkill());
        this.skills.register(new UrlOpenSkill());
    }

    /**
     * Execute a free-form command string by dispatching to registered skills.
     */
    public void executeParsedCommand(String commandText) {
        if (commandText == null)
            return;
        String cmd = commandText.toLowerCase();

        boolean handled = skills.execute(cmd, this);
        if (!handled) {
            // Fallback to raw root execution
            executeRoot(commandText);
        }
    }

    /**
     * Execute a command as root and return output (logged).
     */
    public @Nullable String executeRoot(String rawCommand) {
        try {
            String out = Utils.runRootCommand(rawCommand);
            log("[root] " + rawCommand + " => " + (out != null ? out.trim() : "(no output)"));
            return out;
        } catch (Exception e) {
            log("Root command failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Open an app by package name using multiple methods
     */
    public void openApp(String packageName) {
        try {
            // Method 1: Try using ACTION_VIEW with app scheme (like WhatsApp method)
            if (tryOpenWithScheme(packageName)) {
                return;
            }

            // Method 2: Try using package manager launch intent
            if (Utils.isPackageInstalled(context, packageName)) {
                Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    try {
                        context.startActivity(launch);
                        log("Opened " + packageName + " using launch intent");
                        return;
                    } catch (Exception e) {
                        log("Launch intent failed: " + e.getMessage());
                    }
                }
            }

            // Method 2: Try using root command with monkey
            log("Trying root monkey command for " + packageName);
            String result = executeRoot("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1");
            if (result != null && !result.contains("Error")) {
                log("Opened " + packageName + " using monkey");
                return;
            }

            // Method 3: Try am start with full intent
            log("Trying root am start with intent for " + packageName);
            String amResult = executeRoot(
                    "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER " + packageName);
            if (amResult != null && !amResult.contains("Error")) {
                log("Opened " + packageName + " using am start");
                return;
            }

            // Method 4: Try using input tap on app icon (if we can find it)
            log("Trying to bypass restrictions with input tap");
            executeRoot("input tap 500 500"); // Generic tap, then try monkey again
            Thread.sleep(100);
            executeRoot("monkey -p " + packageName + " 1");

            // Method 5: Last resort - direct package start
            log("Last resort: direct package start");
            executeRoot("am start " + packageName);

        } catch (Exception e) {
            log("openApp error: " + e.getMessage());
        }
    }

    /**
     * Try to open app using ACTION_VIEW with app-specific schemes (like WhatsApp
     * method)
     * This bypasses Android's app interaction blocking
     */
    private boolean tryOpenWithScheme(String packageName) {
        try {
            // Common app schemes that bypass blocking
            String[] schemes = {
                    packageName + "://", // Generic app scheme
                    "intent://" + packageName, // Intent scheme
                    "market://details?id=" + packageName, // Play Store
                    "https://play.google.com/store/apps/details?id=" + packageName // Play Store web
            };

            for (String scheme : schemes) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(scheme));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setPackage(packageName); // Try to force the specific app
                    context.startActivity(intent);
                    log("Opened " + packageName + " using scheme: " + scheme);
                    return true;
                } catch (Exception e) {
                    // Try without forcing package
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(scheme));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        log("Opened " + packageName + " using scheme (no package): " + scheme);
                        return true;
                    } catch (Exception e2) {
                        // Continue to next scheme
                    }
                }
            }
        } catch (Exception e) {
            log("Scheme method failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Send a WhatsApp message via intent (best-effort). This requires WhatsApp
     * installed.
     * For a rooted variant, you could use `am start` with a prepared intent via
     * shell.
     */
    public void sendWhatsAppMessage(String contactOrNumber, String message) {
        try {
            if (!Utils.isPackageInstalled(context, "com.whatsapp")) {
                log("WhatsApp not installed");
                return;
            }

            // Attempt to open WhatsApp chat using URL. contactOrNumber should be phone
            // number in international format.
            String phone = contactOrNumber.replaceAll("[^0-9+]", "");
            if (phone.isEmpty()) {
                log("No phone number parsed for WhatsApp message");
                return;
            }
            String url = "https://api.whatsapp.com/send?phone=" + phone + "&text="
                    + Uri.encode(message == null ? "" : message);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setPackage("com.whatsapp");
            i.setData(Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            log("Sent WhatsApp message (intent) to " + phone);
        } catch (Exception e) {
            log("sendWhatsAppMessage error: " + e.getMessage());
        }
    }

    /**
     * Toggle Wi-Fi. If not allowed by API, will attempt root command.
     */
    public void toggleWiFi(boolean enable) {
        try {
            // Try using settings (may require permissions), otherwise fallback to root
            String cmd = "svc wifi " + (enable ? "enable" : "disable");
            String out = executeRoot(cmd);
            log("toggleWiFi -> " + cmd + " (out=" + (out != null ? out.trim() : "") + ")");
        } catch (Exception e) {
            log("toggleWiFi error: " + e.getMessage());
        }
    }

    /**
     * Kill an app by package name using root.
     */
    public void killApp(String packageName) {
        try {
            executeRoot("am force-stop " + packageName);
            log("Killed app " + packageName);
        } catch (Exception e) {
            log("killApp error: " + e.getMessage());
        }
    }

    public void log(String s) {
        Log.i(TAG, s);
        if (MainActivity.instance != null)
            MainActivity.instance.addLog(s);
    }

    public Context getContext() {
        return context;
    }
}
