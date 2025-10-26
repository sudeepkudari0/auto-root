package com.assistant.root;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * AccessibilityService that enables advanced system interactions
 * This service provides the capability to interact with UI elements across apps
 * when combined with root access.
 */
public class AssistantAccessibilityService extends AccessibilityService {

    private static final String TAG = "AssistantA11yService";
    private static AssistantAccessibilityService instance;

    public static AssistantAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "Accessibility Service created");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to respond to accessibility events for now
        // This service is mainly used for its system-level permissions
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "Service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Accessibility Service connected");

        // Notify MainActivity if it's active
        if (MainActivity.instance != null) {
            MainActivity.instance.addLog("✓ Accessibility Service connected");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.i(TAG, "Accessibility Service destroyed");
    }

    /**
     * Utility method to perform global actions like going home, back, etc.
     * Call this from other parts of your app when needed.
     */
    public boolean doGlobalAction(int action) {
        return performGlobalAction(action);
    }
}
