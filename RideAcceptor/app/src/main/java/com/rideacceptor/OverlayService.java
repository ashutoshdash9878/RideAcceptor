package com.rideacceptor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoDismiss;
    private static final int AUTO_DISMISS_MS = 12000; // 12 seconds
    private static final String CHANNEL_ID = "ride_acceptor_channel";
    private TextView timerText;
    private long alertStartTime;
    private Runnable timerRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(1, buildForegroundNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case "START":
                // Service started, just keep running
                break;
            case "SHOW_ALERT":
                String appName = intent.getStringExtra("app_name");
                String title   = intent.getStringExtra("title");
                String text    = intent.getStringExtra("text");
                String pkg     = intent.getStringExtra("pkg");
                handler.post(() -> showOverlay(appName, title, text, pkg));
                break;
            case "DISMISS":
                handler.post(this::dismissOverlay);
                break;
        }
        return START_STICKY;
    }

    private void showOverlay(String appName, String notifTitle, String notifText, String pkg) {
        dismissOverlay(); // remove any existing overlay
        vibrate();
        alertStartTime = System.currentTimeMillis();

        // Root container - semi-transparent full bottom sheet style
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F0111111"));
        root.setPadding(30, 28, 30, 36);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        // Drag handle bar
        View handle = new View(this);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(80, 6);
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = 20;
        handle.setLayoutParams(hlp);
        handle.setBackgroundColor(Color.parseColor("#444444"));
        root.addView(handle);

        // App badge
        TextView badge = new TextView(this);
        badge.setText("📱 " + appName + " — NEW BOOKING");
        badge.setTextSize(12);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setLetterSpacing(0.08f);
        badge.setTextColor(Color.parseColor("#00e676"));
        badge.setBackgroundColor(Color.parseColor("#1a3a2a"));
        badge.setPadding(24, 10, 24, 10);
        badge.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.bottomMargin = 16;
        badge.setLayoutParams(blp);
        root.addView(badge);

        // Notification title
        if (notifTitle != null && !notifTitle.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(notifTitle);
            tv.setTextSize(15);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.bottomMargin = 6;
            tv.setLayoutParams(tlp);
            root.addView(tv);
        }

        // Notification body text
        if (notifText != null && !notifText.isEmpty()) {
            TextView tv2 = new TextView(this);
            tv2.setText(notifText);
            tv2.setTextSize(13);
            tv2.setTextColor(Color.parseColor("#aaaaaa"));
            tv2.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams t2lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            t2lp.bottomMargin = 20;
            tv2.setLayoutParams(t2lp);
            root.addView(tv2);
        }

        // Timer countdown text
        timerText = new TextView(this);
        timerText.setText("⏱ 12s remaining");
        timerText.setTextSize(12);
        timerText.setTextColor(Color.parseColor("#00e676"));
        timerText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tmlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tmlp.bottomMargin = 16;
        timerText.setLayoutParams(tmlp);
        root.addView(timerText);

        // ---- BIG ACCEPT BUTTON ----
        Button acceptBtn = new Button(this);
        acceptBtn.setText("✅  ACCEPT RIDE");
        acceptBtn.setTextSize(22);
        acceptBtn.setTypeface(null, Typeface.BOLD);
        acceptBtn.setLetterSpacing(0.05f);
        acceptBtn.setBackgroundColor(Color.parseColor("#00e676"));
        acceptBtn.setTextColor(Color.BLACK);
        acceptBtn.setPadding(20, 30, 20, 30);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        alp.bottomMargin = 14;
        acceptBtn.setLayoutParams(alp);
        acceptBtn.setOnClickListener(v -> {
            long reactionMs = System.currentTimeMillis() - alertStartTime;
            openRideApp(pkg);
            dismissOverlay();
            showToast(String.format("Opened in %.1fs ⚡", reactionMs / 1000f));
        });
        root.addView(acceptBtn);

        // ---- DISMISS BUTTON ----
        Button dismissBtn = new Button(this);
        dismissBtn.setText("✗  Skip this booking");
        dismissBtn.setTextSize(14);
        dismissBtn.setBackgroundColor(Color.parseColor("#2a1a1a"));
        dismissBtn.setTextColor(Color.parseColor("#ff1744"));
        dismissBtn.setPadding(20, 16, 20, 16);
        dismissBtn.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        dismissBtn.setOnClickListener(v -> dismissOverlay());
        root.addView(dismissBtn);

        // Window layout params
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM;

        overlayView = root;
        windowManager.addView(overlayView, params);

        // Live timer countdown
        startTimerCountdown();

        // Auto-dismiss after timeout
        autoDismiss = this::dismissOverlay;
        handler.postDelayed(autoDismiss, AUTO_DISMISS_MS);
    }

    private void startTimerCountdown() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timerText == null || overlayView == null) return;
                long elapsed = System.currentTimeMillis() - alertStartTime;
                long remaining = (AUTO_DISMISS_MS - elapsed) / 1000;
                if (remaining < 0) remaining = 0;
                timerText.setText("⏱ " + remaining + "s remaining");
                if (remaining <= 3) timerText.setTextColor(Color.parseColor("#ff1744"));
                else if (remaining <= 6) timerText.setTextColor(Color.parseColor("#ffab00"));
                else timerText.setTextColor(Color.parseColor("#00e676"));
                if (remaining > 0) handler.postDelayed(this, 500);
            }
        };
        handler.postDelayed(timerRunnable, 500);
    }

    private void openRideApp(String pkg) {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                     Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(launchIntent);
            }
        } catch (Exception e) {
            // App not installed or can't open
        }
    }

    private void dismissOverlay() {
        if (autoDismiss != null) handler.removeCallbacks(autoDismiss);
        if (timerRunnable != null) handler.removeCallbacks(timerRunnable);
        timerText = null;
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {}
            overlayView = null;
        }
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(
                new long[]{0, 150, 80, 150, 80, 300}, -1));
        } else {
            v.vibrate(new long[]{0, 150, 80, 150, 80, 300}, -1);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Ride Acceptor", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Keeps the service running");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
            .setContentTitle("⚡ Ride Acceptor is ACTIVE")
            .setContentText("Watching for Uber, Rapido, Ola & Loadshare bookings")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .build();
    }

    private void showToast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissOverlay();
    }
}
