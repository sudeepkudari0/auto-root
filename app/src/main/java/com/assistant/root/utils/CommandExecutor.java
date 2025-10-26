package com.assistant.root.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.assistant.root.skills.base.Skill;
import com.assistant.root.skills.base.SkillRegistry;
import com.assistant.root.skills.communication.WhatsAppSkill;
import com.assistant.root.skills.communication.WhatsAppContextSkill;
import com.assistant.root.skills.system.ListAppsSkill;
import com.assistant.root.skills.system.OpenAppSkill;
import com.assistant.root.skills.system.SystemAppSkill;
import com.assistant.root.skills.system.ToggleWiFiSkill;
import com.assistant.root.skills.web.UrlOpenSkill;
import com.assistant.root.ui.activities.MainActivity;
import com.assistant.root.ai.AICommandGenerator;
import com.assistant.root.ai.RootCommandExecutor;
import com.assistant.root.skills.ai.AISkill;
import com.assistant.root.cache.SmartCommandManager;
import com.assistant.root.context.ContextAwareCommandSystem;
import com.assistant.root.context.HybridCommandSystem;

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
    private final AICommandGenerator aiGenerator;
    private final ContactManager contactManager;
    private final SmartCommandManager smartCommandManager;
    private final ContextAwareCommandSystem contextAwareSystem;
    private final HybridCommandSystem hybridSystem;
    private boolean isAIProcessing = false;

    // Callback interface for overlay updates
    public interface OverlayUpdateCallback {
        void updateOverlayMessage(String message);
    }

    private OverlayUpdateCallback overlayCallback;

    public CommandExecutor(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.skills = new SkillRegistry();
        this.aiGenerator = new AICommandGenerator(ctx);
        this.contactManager = new ContactManager(ctx);
        this.smartCommandManager = new SmartCommandManager(ctx);
        this.contextAwareSystem = new ContextAwareCommandSystem(ctx);
        this.hybridSystem = new HybridCommandSystem(ctx);

        // Register built-in skills. Add more skills by creating classes implementing
        // Skill.
        this.skills.register(new OpenAppSkill());
        // this.skills.register(new WhatsAppContextSkill()); // Disabled - using
        // universal context-aware AI
        // this.skills.register(new WhatsAppSkill()); // Temporarily disabled - let AI
        // handle all messaging
        this.skills.register(new ToggleWiFiSkill());
        // this.skills.register(new ListAppsSkill());
        // this.skills.register(new SystemAppSkill());
        // this.skills.register(new UrlOpenSkill());
        this.skills.register(new AISkill());

        // Check and request contacts permission
        checkContactsPermission();

        // Log contact information for debugging
        logContactInfo();

        // Warm up cache in background
        smartCommandManager.warmupCache();
    }

    /**
     * Execute a free-form command string by dispatching to registered skills.
     */
    public void executeParsedCommand(String commandText) {
        if (commandText == null)
            return;
        String cmd = commandText.toLowerCase();

        log("🔍 Processing command: " + commandText);

        // Show current context for every command (async to avoid blocking)
        showContextAsync();

        boolean handled = skills.execute(cmd, this);
        if (!handled) {
            log("⚠️ No skill matched, using raw root execution");
            // Fallback to raw root execution
            executeRoot(commandText);
        }
    }

    /**
     * Show context information asynchronously to avoid blocking the main thread
     */
    private void showContextAsync() {
        new Thread(() -> {
            try {
                log("🔍 Starting context detection...");
                String contextInfo = getCurrentContextInfo();
                if (contextInfo != null && !contextInfo.isEmpty()) {
                    log("📱 Current Context:\n" + contextInfo);

                    // Show available actions if in a supported app
                    if (supportsQuickCommands()) {
                        log("🎯 App supports quick commands, getting available actions...");
                        String availableActions = getAvailableActions();
                        if (availableActions != null && !availableActions.isEmpty()) {
                            log("🎯 Available Actions:\n" + availableActions);
                        } else {
                            log("⚠️ No available actions found");
                        }
                    } else {
                        log("ℹ️ App doesn't support quick commands");
                    }
                } else {
                    log("⚠️ Context detection returned null or empty");
                }
            } catch (Exception e) {
                log("❌ Error getting context: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
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

    /**
     * Get all installed packages using su command
     * Returns a list of package names
     */
    public List<String> getAllInstalledPackages() {
        List<String> packages = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            os.writeBytes("pm list packages\n");
            os.writeBytes("exit\n");
            os.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("package:")) {
                    String packageName = line.substring(8); // Remove "package:" prefix
                    packages.add(packageName);
                }
            }

            process.waitFor();
            reader.close();
            os.close();

            log("Found " + packages.size() + " installed packages");

        } catch (Exception e) {
            log("Error getting installed packages: " + e.getMessage());
        }

        return packages;
    }

    /**
     * Find app package by name using su command to get all packages
     * and then matching with user input
     */
    public String findAppPackageByName(String appName) {
        List<String> packages = getAllInstalledPackages();
        String appNameLower = appName.toLowerCase();

        // First pass: exact matches
        for (String packageName : packages) {
            if (packageName.toLowerCase().contains(appNameLower)) {
                return packageName;
            }
        }

        // Second pass: fuzzy matching with common app names
        String[] commonAppNames = {
                "whatsapp", "telegram", "chrome", "browser", "firefox", "safari", "edge",
                "youtube", "instagram", "facebook", "twitter", "tiktok", "snapchat",
                "gmail", "outlook", "mail", "calendar", "clock", "calculator",
                "camera", "gallery", "photos", "music", "spotify", "netflix",
                "maps", "google maps", "waze", "uber", "settings", "contacts",
                "phone", "messages", "sms", "dialer", "files", "file manager",
                "notes", "notepad", "keep", "evernote", "pdf", "reader",
                "office", "word", "excel", "powerpoint", "adobe", "photoshop"
        };

        for (String commonName : commonAppNames) {
            if (appNameLower.contains(commonName)) {
                for (String packageName : packages) {
                    if (packageName.toLowerCase().contains(commonName)) {
                        return packageName;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Open app using su command with monkey or am start
     */
    public void openAppWithSu(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            // Try monkey command first
            os.writeBytes("monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1\n");
            os.writeBytes("exit\n");
            os.flush();

            process.waitFor();
            os.close();

            log("Opened " + packageName + " using su monkey command");

        } catch (Exception e) {
            log("Error opening app with su: " + e.getMessage());

            // Fallback to am start
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());

                os.writeBytes("am start -n " + packageName + "/.MainActivity\n");
                os.writeBytes("exit\n");
                os.flush();

                process.waitFor();
                os.close();

                log("Opened " + packageName + " using su am start command");

            } catch (Exception e2) {
                log("Error with am start fallback: " + e2.getMessage());
            }
        }
    }

    /**
     * Generate and execute AI command from natural language input
     * Uses SmartCommandManager for caching to eliminate delays
     */
    public void executeAICommand(String userInput) {
        log("🤖 Processing AI command: " + userInput);
        isAIProcessing = true;
        updateOverlay("🤖 Processing...");

        // Use SmartCommandManager for caching (original working method)
        smartCommandManager.getCommand(userInput, new SmartCommandManager.CommandCallback() {
            @Override
            public void onCommandReady(String command, boolean fromCache) {
                if (fromCache) {
                    log("⚡ Cache HIT! Instant execution");
                    updateOverlay("⚡ Executing cached command...");
                } else {
                    log("✅ AI Generated Command:\n" + command);
                    log("⚡ Executing AI-generated commands...");
                    updateOverlay("⚡ Executing commands...");
                }
                executeAIGeneratedCommand(command);
            }

            @Override
            public void onError(String error) {
                log("❌ AI Error: " + error);
                updateOverlay("❌ AI Error");
                isAIProcessing = false;
            }
        });
    }

    /**
     * Execute AI command with full context awareness (alternative method)
     */
    public void executeContextAwareCommand(String userInput) {
        log("🧠 Processing context-aware command: " + userInput);
        isAIProcessing = true;
        updateOverlay("🧠 Analyzing context...");

        // Get current context info
        String contextInfo = contextAwareSystem.getCurrentContextInfo();
        log("📱 Current Context:\n" + contextInfo);

        // Use full context-aware processing
        contextAwareSystem.quickExecute(userInput, new ContextAwareCommandSystem.SystemCallback() {
            @Override
            public void onCommandReady(String command, boolean fromCache, String contextInfo) {
                if (fromCache) {
                    log("⚡ Context-aware cache HIT!");
                    updateOverlay("⚡ Executing cached command...");
                } else {
                    log("✅ Context-aware AI Generated Command:\n" + command);
                    updateOverlay("⚡ Executing commands...");
                }
            }

            @Override
            public void onExecutionProgress(String step) {
                log("🔄 " + step);
                updateOverlay("🔄 " + step);
            }

            @Override
            public void onExecutionComplete(String result) {
                log("✅ Context-aware execution completed: " + result);
                updateOverlay("✅ Done!");
                isAIProcessing = false;
            }

            @Override
            public void onError(String error) {
                log("❌ Context-aware execution failed: " + error);
                updateOverlay("❌ Failed");
                isAIProcessing = false;
            }
        });
    }

    /**
     * Execute a command using the new hybrid system (instant patterns + AI
     * fallback)
     */
    public void executeHybridCommand(String userInput) {
        log("🚀 Using hybrid system for: " + userInput);
        isAIProcessing = true;
        updateOverlay("🚀 Processing with hybrid system...");

        hybridSystem.processCommand(userInput, new HybridCommandSystem.SystemCallback() {
            @Override
            public void onCommandReady(String command, boolean instant, String source) {
                log("✅ Hybrid command ready from " + source + ": " + command);
                updateOverlay("⚡ Executing " + source + " command...");
                executeRoot(command);
                isAIProcessing = false;
            }

            @Override
            public void onError(String error) {
                log("❌ Hybrid system error: " + error);
                updateOverlay("❌ Hybrid system failed");
                isAIProcessing = false;
                // Fallback to regular AI
                executeAICommand(userInput);
            }
        });
    }

    /**
     * Execute AI-generated command using RootCommandExecutor
     */
    private void executeAIGeneratedCommand(String command) {
        log("🔧 Executing AI-generated commands with root privileges...");
        updateOverlay("🔧 Running commands...");

        // Replace contact names with phone numbers
        String processedCommand = replaceContactNames(command);
        if (!processedCommand.equals(command)) {
            log("📞 Replaced contact names with phone numbers");
            log("📋 Processed command:\n" + processedCommand);
        }

        RootCommandExecutor.executeMultipleCommands(processedCommand, new RootCommandExecutor.ExecutionCallback() {
            @Override
            public void onSuccess(String output) {
                log("✅ AI command execution completed successfully!");
                if (!output.trim().isEmpty()) {
                    log("📋 Command output:\n" + output);
                }
                log("🎉 Task completed!");
                updateOverlay("✅ Done!");
                isAIProcessing = false;
            }

            @Override
            public void onError(String error) {
                log("❌ AI command execution failed: " + error);
                log("💡 You may need to check root permissions or command syntax");
                updateOverlay("❌ Failed");
                isAIProcessing = false;
            }
        });
    }

    /**
     * Replace contact names with phone numbers in commands
     */
    private String replaceContactNames(String command) {
        if (command == null || command.isEmpty()) {
            return command;
        }

        String processedCommand = command;

        // Look for WhatsApp URL patterns with contact names
        // Pattern: https://api.whatsapp.com/send?phone=CONTACT_NAME&text=MESSAGE
        String pattern = "https://api\\.whatsapp\\.com/send\\?phone=([^&]+)&text=([^']+)";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(processedCommand);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String contactName = matcher.group(1);
            String message = matcher.group(2);

            // Get phone number for the contact
            String phoneNumber = contactManager.getPhoneNumber(contactName);

            if (phoneNumber != null) {
                // Replace contact name with phone number
                String replacement = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + message;
                matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
                log("📞 Found contact: " + contactName + " -> " + phoneNumber);
            } else {
                // Keep original if contact not found
                matcher.appendReplacement(result, matcher.group(0));
                log("⚠️ Contact not found: " + contactName);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Check if device has root access
     */
    public boolean hasRootAccess() {
        return RootCommandExecutor.hasRootAccess();
    }

    /**
     * Execute multiple commands using RootCommandExecutor
     */
    public void executeMultipleCommands(String commands) {
        RootCommandExecutor.executeMultipleCommands(commands, new RootCommandExecutor.ExecutionCallback() {
            @Override
            public void onSuccess(String output) {
                log("✅ Commands executed successfully!");
                if (!output.trim().isEmpty()) {
                    log("Output:\n" + output);
                }
            }

            @Override
            public void onError(String error) {
                log("❌ Commands execution failed: " + error);
            }
        });
    }

    /**
     * Test AI functionality with a simple command
     */
    public void testAI(String testInput) {
        log("🧪 Testing AI with: " + testInput);

        if (!aiGenerator.isModelAvailable()) {
            log("❌ AI model not available for testing");
            return;
        }

        aiGenerator.testAI(testInput, new AICommandGenerator.CommandCallback() {
            @Override
            public void onCommandGenerated(String command) {
                log("🧪 AI Test Result: " + command);
            }

            @Override
            public void onError(String error) {
                log("❌ AI Test Error: " + error);
            }
        });
    }

    /**
     * Set callback for overlay updates
     */
    public void setOverlayCallback(OverlayUpdateCallback callback) {
        this.overlayCallback = callback;
    }

    /**
     * Update overlay message if callback is set
     */
    public void updateOverlay(String message) {
        if (overlayCallback != null) {
            overlayCallback.updateOverlayMessage(message);
        }
    }

    /**
     * Check if AI is currently processing
     */
    public boolean isAIProcessing() {
        return isAIProcessing;
    }

    /**
     * Check contacts permission and request if needed
     */
    private void checkContactsPermission() {
        if (contactManager.hasContactsPermission()) {
            log("✅ Contacts permission already granted");
        } else {
            log("⚠️ Contacts permission not granted - requesting permission...");
            requestContactsPermission();
        }
    }

    /**
     * Request contacts permission
     */
    private void requestContactsPermission() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                log("📞 Requesting contacts permission...");
                ActivityCompat.requestPermissions(activity,
                        new String[] { Manifest.permission.READ_CONTACTS },
                        1001); // Request code for contacts
            }
        } else {
            log("⚠️ Cannot request permission - context is not an Activity");
        }
    }

    /**
     * Handle permission result (call this from Activity's
     * onRequestPermissionsResult)
     */
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1001) { // Contacts permission request
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                log("✅ Contacts permission granted! Loading contacts...");
                contactManager.loadContactsAfterPermissionGranted();
                logContactInfo();
            } else {
                log("❌ Contacts permission denied. Contact features will not work.");
            }
        }
    }

    /**
     * Get contact information for debugging
     */
    public void logContactInfo() {
        if (contactManager.hasContactsPermission()) {
            int contactCount = contactManager.getContactCount();
            log("📱 Total contacts loaded: " + contactCount);

            if (contactCount > 0) {
                List<String> contactNames = contactManager.getAllContactNames();
                log("📋 Sample contacts: " + contactNames.subList(0, Math.min(5, contactNames.size())));
            } else {
                log("📋 No contacts found in device");
            }
        } else {
            log("⚠️ Contacts permission not granted - cannot access contacts");
        }
    }

    /**
     * Log all contacts for debugging
     */
    public void logAllContacts() {
        contactManager.logAllContacts();
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return smartCommandManager.getCacheStats();
    }

    /**
     * Clear command cache
     */
    public void clearCache() {
        smartCommandManager.clearCache();
        log("🗑️ Command cache cleared");
    }

    /**
     * Clean old cache entries
     */
    public void cleanupCache() {
        smartCommandManager.cleanupCache();
        log("🧹 Cache cleaned up");
    }

    /**
     * Preload predicted commands based on current context
     */
    public void preloadPredictedCommands(String currentContext) {
        smartCommandManager.preloadPredictedCommands(currentContext);
        log("🔮 Preloading predicted commands for: " + currentContext);
    }

    /**
     * Get current context information
     */
    public String getCurrentContextInfo() {
        return contextAwareSystem.getCurrentContextInfo();
    }

    /**
     * Get available actions in current context
     */
    public String getAvailableActions() {
        return contextAwareSystem.getAvailableActions();
    }

    /**
     * Get app-specific help
     */
    public String getAppHelp() {
        return contextAwareSystem.getAppHelp();
    }

    /**
     * Get common actions for current app
     */
    public String[] getCommonActions() {
        return contextAwareSystem.getCommonActions();
    }

    /**
     * Check if current app supports quick commands
     */
    public boolean supportsQuickCommands() {
        return contextAwareSystem.supportsQuickCommands();
    }

    /**
     * Get system status
     */
    public String getSystemStatus() {
        return contextAwareSystem.getSystemStatus();
    }

    /**
     * Warm up cache for current app
     */
    public void warmupCurrentAppCache() {
        contextAwareSystem.warmupCurrentAppCache();
        log("🔥 Warming up cache for current app...");
    }

    /**
     * Test context detection manually
     */
    public void testContextDetection() {
        log("🧪 Testing context detection...");
        new Thread(() -> {
            try {
                String contextInfo = getCurrentContextInfo();
                if (contextInfo != null && !contextInfo.isEmpty()) {
                    log("✅ Context detection successful:\n" + contextInfo);
                } else {
                    log("❌ Context detection failed - returned null or empty");
                }
            } catch (Exception e) {
                log("❌ Context detection error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public Context getContext() {
        return context;
    }
}
