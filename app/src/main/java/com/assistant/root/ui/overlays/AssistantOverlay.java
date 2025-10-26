package com.assistant.root.ui.overlays;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.assistant.root.R;

import java.util.Locale;

/**
 * Google Assistant-like overlay that appears when wake word is detected
 */
public class AssistantOverlay {

    private static final String TAG = "AssistantOverlay";

    private Context context;
    private WindowManager windowManager;
    private View overlayView;
    private TextView tvAssistantText;
    private ImageView ivMicrophone;
    private TextToSpeech tts;

    private boolean isShowing = false;
    private Handler handler = new Handler();

    public AssistantOverlay(Context context) {
        this.context = context;
        initializeTTS();
    }

    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS: Language not supported");
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    public boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    public void show(String message) {
        showInternal(message, true);
    }

    public void showWithoutSpeaking(String message) {
        showInternal(message, false);
    }

    private void showInternal(String message, boolean shouldSpeak) {
        if (isShowing) {
            // Update existing overlay
            updateMessage(message, shouldSpeak);
            return;
        }

        if (!checkOverlayPermission()) {
            Log.e(TAG, "Overlay permission not granted");
            return;
        }

        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            overlayView = inflater.inflate(R.layout.overlay_assistant, null);

            tvAssistantText = overlayView.findViewById(R.id.tvAssistantText);
            ivMicrophone = overlayView.findViewById(R.id.ivMicrophone);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.BOTTOM;
            params.y = 100;

            windowManager.addView(overlayView, params);

            tvAssistantText.setText(message);

            // Animate in
            overlayView.setAlpha(0f);
            overlayView.setTranslationY(100f);
            overlayView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            // Pulse animation for microphone
            startMicrophonePulse();

            isShowing = true;
            Log.i(TAG, "Overlay shown");

            // Optionally speak the message
            if (shouldSpeak) {
                speak(message);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing overlay: " + e.getMessage());
        }
    }

    public void updateMessage(String message) {
        updateMessage(message, false);
    }

    public void updateMessage(String message, boolean shouldSpeak) {
        if (tvAssistantText != null) {
            handler.post(() -> {
                tvAssistantText.setText(message);
                if (shouldSpeak) {
                    speak(message);
                }
            });
        }
    }

    private void startMicrophonePulse() {
        if (ivMicrophone == null)
            return;

        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(1f, 1.2f);
        pulseAnimator.setDuration(800);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            ivMicrophone.setScaleX(scale);
            ivMicrophone.setScaleY(scale);
        });
        pulseAnimator.start();
    }

    public void hide() {
        if (!isShowing || overlayView == null)
            return;

        overlayView.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        removeOverlay();
                    }
                })
                .start();
    }

    private void removeOverlay() {
        try {
            if (windowManager != null && overlayView != null) {
                windowManager.removeView(overlayView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing overlay: " + e.getMessage());
        }
        overlayView = null;
        isShowing = false;
        Log.i(TAG, "Overlay hidden");
    }

    public void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void destroy() {
        hide();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public boolean isShowing() {
        return isShowing;
    }
}
