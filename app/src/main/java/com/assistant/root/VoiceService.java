package com.assistant.root;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Voice Assistant Service - Quick Command Mode
 * 
 * Shows floating button. Click button to give voice commands directly.
 * No wake word needed!
 */
public class VoiceService extends Service {

    private static final String TAG = "VoiceService";
    private static final String CHANNEL_ID = "voice_service_channel";
    private static final int NOTIF_ID = 1337;

    private SpeechRecognizer speechRecognizer;
    private CommandExecutor executor;
    private AssistantOverlay assistantOverlay;
    private FloatingButton floatingButton;

    private Handler handler = new Handler();
    private Runnable commandTimeoutRunnable;

    // State management
    private volatile boolean isServiceRunning = true;
    private volatile boolean isListening = false;

    private int commandTimeoutMs = 15000; // 15 seconds to give command

    // Track last heard command from partial results
    private String lastPartialResult = "";

    @Override
    public void onCreate() {
        super.onCreate();

        executor = new CommandExecutor(this);
        assistantOverlay = new AssistantOverlay(this);
        floatingButton = new FloatingButton(this);

        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification("Ready"));

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            log("ERROR: Speech recognition not available.");
            stopSelf();
            return;
        }

        log("✓ Voice Assistant ready!");
        log("Tap the floating button to give commands.");

        recreateSpeechRecognizer();

        // Show floating button
        floatingButton.show(() -> {
            // Button clicked - start listening for command
            onFloatingButtonClicked();
        });

        updateNotification("Tap button to speak");
    }

    private void onFloatingButtonClicked() {
        log("🎤 Button clicked - Ready for command!");

        // Show overlay without speaking
        assistantOverlay.showWithoutSpeaking("Listening...");

        // Start listening immediately
        handler.postDelayed(() -> {
            startListeningForCommand();
        }, 300);
    }

    private void recreateSpeechRecognizer() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
        } catch (Exception ignored) {
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new VoiceRecognitionListener());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        log("Voice Assistant stopped.");

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (assistantOverlay != null) {
            assistantOverlay.destroy();
        }

        if (floatingButton != null) {
            floatingButton.hide();
        }

        handler.removeCallbacksAndMessages(null);
    }

    private void startListeningForCommand() {
        if (!isServiceRunning || isListening)
            return;

        isListening = true;
        lastPartialResult = ""; // Reset partial results

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

            // Command mode: longer timeouts so user has time to speak
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000); // 3 seconds of
                                                                                                       // silence before
                                                                                                       // stopping
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0);

            log("🎧 Listening for your command...");
            updateNotification("Listening...");

            speechRecognizer.startListening(intent);

            // Set timeout for command listening
            commandTimeoutRunnable = () -> {
                log("⏱️ Command timeout");
                assistantOverlay.hide();
                isListening = false;
                updateNotification("Tap button to speak");
            };
            handler.postDelayed(commandTimeoutRunnable, commandTimeoutMs);

        } catch (Exception e) {
            log("ERROR: Failed to start listening: " + e.getMessage());
            isListening = false;
            assistantOverlay.hide();
            updateNotification("Tap button to speak");
        }
    }

    private Notification buildNotification(String text) {
        Intent piIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, piIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Assistant")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, buildNotification(text));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Voice Service",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private void log(String s) {
        Log.i(TAG, s);
        if (MainActivity.instance != null) {
            MainActivity.instance.addLog(s);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Recognition listener for command processing
     */
    private class VoiceRecognitionListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            log("✓ Ready - Speak your command now!");
        }

        @Override
        public void onBeginningOfSpeech() {
            log("🔊 Speech detected...");
        }

        @Override
        public void onEndOfSpeech() {
            log("🔇 Processing command...");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Audio level monitoring (optional)
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onError(int error) {
            String errorMsg = getErrorMessage(error);

            if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                log("⚠️ Could not understand the command");
                assistantOverlay.updateMessage("Didn't catch that. Try again!");
                handler.postDelayed(() -> {
                    assistantOverlay.hide();
                    isListening = false;
                    updateNotification("Tap button to speak");
                }, 2000);
                return;
            }

            if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                log("⏱️ No speech detected");
                assistantOverlay.hide();
                isListening = false;
                updateNotification("Tap button to speak");
                return;
            }

            if (error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                log("ERROR: " + errorMsg + " - recreating recognizer");
                recreateSpeechRecognizer();
                assistantOverlay.hide();
                isListening = false;
                updateNotification("Tap button to speak");
                return;
            }

            log("ERROR: " + errorMsg);
            assistantOverlay.hide();
            isListening = false;
            updateNotification("Tap button to speak");
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);

            handler.removeCallbacks(commandTimeoutRunnable);

            String command = null;

            // Try to get command from results
            if (matches != null && !matches.isEmpty()) {
                command = matches.get(0);
            }
            // If no results but we have partial results, use that
            else if (!lastPartialResult.isEmpty()) {
                command = lastPartialResult;
                log("ℹ️ Using last partial result as command");
            }

            if (command == null || command.trim().isEmpty()) {
                log("⚠️ No command detected");
                assistantOverlay.updateMessage("Didn't hear anything. Try again!");
                handler.postDelayed(() -> {
                    assistantOverlay.hide();
                    isListening = false;
                    updateNotification("Tap button to speak");
                }, 2000);
                return;
            }

            // Execute the command
            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log("COMMAND: \"" + command + "\"");
            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Update overlay with command (no speaking)
            assistantOverlay.updateMessage("\"" + command + "\"", false);

            // Show executing status after a brief moment
            handler.postDelayed(() -> {
                assistantOverlay.updateMessage("Executing...", false);
            }, 500);

            // Execute command
            executor.executeParsedCommand(command);

            // Reset partial result
            lastPartialResult = "";

            // Hide overlay after 3 seconds and reset
            handler.postDelayed(() -> {
                assistantOverlay.hide();
                isListening = false;
                updateNotification("Tap button to speak");
                log("✓ Ready for next command (tap button)");
            }, 3000);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);

            if (matches == null || matches.isEmpty())
                return;

            String partial = matches.get(0);

            // Save the last partial result in case final results are empty
            lastPartialResult = partial;

            // Show partial command in overlay (no speaking)
            assistantOverlay.updateMessage("\"" + partial + "\"", false);
            log("Hearing: \"" + partial + "\"");
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Speech timeout";
            default:
                return "Unknown error (" + error + ")";
        }
    }
}
