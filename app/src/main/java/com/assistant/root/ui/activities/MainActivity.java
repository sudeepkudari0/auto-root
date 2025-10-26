package com.assistant.root.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.assistant.root.R;
import com.assistant.root.services.AssistantAccessibilityService;
import com.assistant.root.services.VoiceService;
import com.assistant.root.utils.Utils;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1002;
    private static final String TAG = "MainActivity";

    private Button btnStartService;
    private Button btnStopService;
    private TextView tvStatus;
    private TextView tvLogs;
    private ScrollView scrollLogs;

    public static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);
        tvStatus = findViewById(R.id.tvStatus);
        tvLogs = findViewById(R.id.tvLogs);
        scrollLogs = findViewById(R.id.scrollLogs);

        btnStartService.setOnClickListener(v -> startVoiceService());
        btnStopService.setOnClickListener(v -> stopVoiceService());

        checkPermissions();

        updateStatus("Ready to start");
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }

        // Check for overlay permission (CRITICAL for Android 15)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialog();
            } else {
                addLog("Overlay permission: ✓ Granted");
            }
        }

        // Check for root access
        if (!Utils.isRooted()) {
            addLog("Warning: Root access not detected.");
            Toast.makeText(this, "Root access required for full functionality",
                    Toast.LENGTH_LONG).show();
        } else {
            addLog("Root access: ✓ Detected");
        }
    }

    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("This app needs overlay permission to work in the background on Android 15.\n\n" +
                        "Without this, speech recognition will not work when the app is minimized.\n\n" +
                        "Click OK to grant permission.")
                .setPositiveButton("OK", (dialog, which) -> requestOverlayPermission())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    addLog("WARNING: Overlay permission denied. Service may not work!");
                    Toast.makeText(this, "Service may not work without overlay permission",
                            Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    addLog("Overlay permission: ✓ Granted");
                    Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    addLog("ERROR: Overlay permission denied!");
                    Toast.makeText(this, "App may not work without overlay permission",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                addLog("Runtime permissions: ✓ All granted");
            } else {
                addLog("ERROR: Some permissions denied!");
                Toast.makeText(this, "Permissions required for app to work",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startVoiceService() {
        // Check permissions first
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio recording permission required",
                    Toast.LENGTH_SHORT).show();
            checkPermissions();
            return;
        }

        // Check overlay permission on Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission required for background operation",
                        Toast.LENGTH_LONG).show();
                showOverlayPermissionDialog();
                return;
            }
        }

        // Check accessibility service (recommended but not required)
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog();
        }

        Intent serviceIntent = new Intent(this, VoiceService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        updateStatus("Service running");
        addLog("✓ Voice Assistant started. Say 'boss' to activate.");

        Toast.makeText(this, "Voice Assistant running! Say 'boss' to activate.",
                Toast.LENGTH_SHORT).show();

        // Auto-minimize app to home screen after 1 second
        new android.os.Handler().postDelayed(() -> {
            moveTaskToBack(true);
        }, 1000);
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + AssistantAccessibilityService.class.getName();
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);

            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

                if (settingValue != null) {
                    return settingValue.contains(service);
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showAccessibilityServiceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Accessibility Service")
                .setMessage("Enable Accessibility Service for advanced features?\n\n" +
                        "This allows the assistant to perform system-level actions.\n\n" +
                        "(Optional but recommended)")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(this, "Find 'Auto Root' and enable it",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Skip", null)
                .show();
    }

    private void stopVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceService.class);
        stopService(serviceIntent);

        updateStatus("Service stopped");
        addLog("Voice Assistant Service stopped");
    }

    public void updateStatus(final String status) {
        runOnUiThread(() -> {
            if (tvStatus != null) {
                tvStatus.setText("Status: " + status);
                Log.d(TAG, "Status: " + status);
            }
        });
    }

    public void addLog(final String log) {
        runOnUiThread(() -> {
            if (tvLogs != null) {
                String timestamp = Utils.getCurrentTimestamp();
                tvLogs.append("[" + timestamp + "] " + log + "\n");
                Log.d(TAG, log);

                if (scrollLogs != null) {
                    scrollLogs.post(() -> scrollLogs.fullScroll(ScrollView.FOCUS_DOWN));
                }
            }
        });
    }

    public void clearLogs() {
        runOnUiThread(() -> {
            if (tvLogs != null) {
                tvLogs.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}