package com.assistant.root.ui.overlays;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.assistant.root.R;

/**
 * Floating button that stays on screen for quick access to voice assistant
 * Similar to Facebook Messenger chat heads
 */
public class FloatingButton {

    private static final String TAG = "FloatingButton";

    private Context context;
    private WindowManager windowManager;
    private View floatingView;
    private ImageView floatingIcon;

    private WindowManager.LayoutParams params;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    private OnClickListener clickListener;

    public interface OnClickListener {
        void onClick();
    }

    public FloatingButton(Context context) {
        this.context = context;
    }

    public boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    public void show(OnClickListener listener) {
        this.clickListener = listener;

        if (!checkOverlayPermission()) {
            Log.e(TAG, "Overlay permission not granted");
            return;
        }

        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            floatingView = inflater.inflate(R.layout.floating_button, null);

            floatingIcon = floatingView.findViewById(R.id.floating_icon);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 100;
            params.y = 300;

            windowManager.addView(floatingView, params);

            // Handle dragging and clicking
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private long pressStartTime;
                private static final int MAX_CLICK_DURATION = 200;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressStartTime = System.currentTimeMillis();
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                            return true;

                        case MotionEvent.ACTION_UP:
                            long pressDuration = System.currentTimeMillis() - pressStartTime;

                            // If it was a quick tap (not a drag), treat as click
                            if (pressDuration < MAX_CLICK_DURATION) {
                                if (clickListener != null) {
                                    clickListener.onClick();
                                }
                            }
                            return true;
                    }
                    return false;
                }
            });

            Log.i(TAG, "Floating button shown");

        } catch (Exception e) {
            Log.e(TAG, "Error showing floating button: " + e.getMessage());
        }
    }

    public void hide() {
        try {
            if (windowManager != null && floatingView != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing floating button: " + e.getMessage());
        }
        floatingView = null;
        Log.i(TAG, "Floating button hidden");
    }

    public boolean isShowing() {
        return floatingView != null;
    }
}
