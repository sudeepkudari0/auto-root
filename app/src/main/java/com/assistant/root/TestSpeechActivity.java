package com.assistant.root;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

/**
 * TestSpeechActivity - Simple test to verify SpeechRecognizer works
 * This will help diagnose if the issue is service-specific or system-wide
 */
public class TestSpeechActivity extends AppCompatActivity {

    private static final String TAG = "TestSpeechActivity";
    private static final int PERMISSION_REQUEST = 100;

    private SpeechRecognizer speechRecognizer;
    private TextView tvTestLog;
    private Button btnStartTest;
    private Button btnStopTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_speech);

        tvTestLog = findViewById(R.id.tvTestLog);
        btnStartTest = findViewById(R.id.btnStartTest);
        btnStopTest = findViewById(R.id.btnStopTest);

        btnStartTest.setOnClickListener(v -> startListening());
        btnStopTest.setOnClickListener(v -> stopListening());

        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST);
        } else {
            initializeSpeechRecognizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeSpeechRecognizer();
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new TestRecognitionListener());
            addLog("Speech recognizer initialized successfully");
        } else {
            addLog("ERROR: Speech recognition not available!");
        }
    }

    private void startListening() {
        if (speechRecognizer == null) {
            addLog("ERROR: Speech recognizer not initialized");
            return;
        }

        addLog("=== STARTING SPEECH RECOGNITION TEST ===");
        addLog("Speak now...");

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);

        // Short timeouts for testing
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);

        try {
            speechRecognizer.startListening(intent);
            addLog("startListening() called successfully");
        } catch (Exception e) {
            addLog("ERROR: Exception starting listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            addLog("Recognition cancelled");
        }
    }

    private void addLog(String message) {
        runOnUiThread(() -> {
            Log.i(TAG, message);
            tvTestLog.append(message + "\n");
        });
    }

    private class TestRecognitionListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            addLog("✓ onReadyForSpeech - Recognizer is ready!");
        }

        @Override
        public void onBeginningOfSpeech() {
            addLog("✓✓✓ onBeginningOfSpeech - SPEECH DETECTED!");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Log only when there's actual sound
            if (rmsdB > 3.0f) {
                addLog("Audio level: " + String.format("%.1f", rmsdB) + " dB");
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            addLog("✓ Audio buffer received: " + buffer.length + " bytes");
        }

        @Override
        public void onEndOfSpeech() {
            addLog("✓ onEndOfSpeech - Speech ended");
        }

        @Override
        public void onError(int error) {
            String errorMsg;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMsg = "ERROR_AUDIO - Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMsg = "ERROR_CLIENT - Client side error";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMsg = "ERROR_INSUFFICIENT_PERMISSIONS - Missing permissions";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMsg = "ERROR_NETWORK - Network error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    errorMsg = "ERROR_NETWORK_TIMEOUT - Network timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMsg = "ERROR_NO_MATCH - No recognition match";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMsg = "ERROR_RECOGNIZER_BUSY - Recognition busy";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    errorMsg = "ERROR_SERVER - Server error";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMsg = "ERROR_SPEECH_TIMEOUT - No speech input";
                    break;
                default:
                    errorMsg = "ERROR_UNKNOWN - Error code: " + error;
            }
            addLog("✗ onError: " + errorMsg);
        }

        @Override
        public void onResults(Bundle results) {
            addLog("✓ onResults - Final results received!");
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                addLog("Results:");
                for (int i = 0; i < matches.size(); i++) {
                    addLog("  " + (i+1) + ". \"" + matches.get(i) + "\"");
                }
            } else {
                addLog("No results returned");
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                addLog("Partial: \"" + matches.get(0) + "\"");
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            addLog("✓ onEvent: " + eventType);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}