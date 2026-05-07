package com.rideacceptor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.ScrollView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildUI(); // refresh status on every resume
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0a0a0a"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 60);
        root.setBackgroundColor(Color.parseColor("#0a0a0a"));

        // Title
        TextView title = new TextView(this);
        title.setText("⚡ RIDE ACCEPTOR");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#00e676"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Accept bookings before other drivers");
        sub.setTextSize(13);
        sub.setTextColor(Color.parseColor("#888888"));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 8, 0, 48);
        root.addView(sub);

        // --- STEP 1: Notification Listener ---
        boolean notifGranted = isNotificationListenerEnabled();
        addStepCard(root, "Step 1", "Notification Access",
            "Allow app to read Uber, Rapido & Ola notifications",
            notifGranted,
            v -> {
                Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(i);
            });

        // --- STEP 2: Overlay Permission ---
        boolean overlayGranted = Settings.canDrawOverlays(this);
        addStepCard(root, "Step 2", "Display Over Other Apps",
            "Shows the ACCEPT button on top of any screen",
            overlayGranted,
            v -> {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivity(i);
            });

        // --- STEP 3: Start Service ---
        addStepCard(root, "Step 3", "Start the Service",
            notifGranted && overlayGranted ?
                "✅ All permissions granted! Tap to activate." :
                "⚠ Grant both permissions above first",
            notifGranted && overlayGranted,
            v -> {
                if (notifGranted && overlayGranted) {
                    Intent i = new Intent(this, OverlayService.class);
                    i.setAction("START");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        startForegroundService(i);
                    else
                        startService(i);
                    showToast("✅ Ride Acceptor is now ACTIVE!");
                } else {
                    showToast("Please grant both permissions first!");
                }
            });

        // Info box
        TextView info = new TextView(this);
        info.setText("How it works:\n\n" +
            "• Runs silently in background\n" +
            "• Detects Uber, Rapido, Ola, Loadshare bookings\n" +
            "• Shows a BIG ACCEPT button on your screen\n" +
            "• Tap it instantly — faster than opening the app\n\n" +
            "Works even when your screen is on another app.");
        info.setTextSize(13);
        info.setTextColor(Color.parseColor("#888888"));
        info.setBackgroundColor(Color.parseColor("#161616"));
        info.setPadding(30, 25, 30, 25);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 32;
        info.setLayoutParams(lp);
        info.setLineSpacing(6, 1);
        // rounded corners via background drawable would need XML; skip for simplicity
        root.addView(info);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void addStepCard(LinearLayout parent, String stepNum, String stepTitle,
                              String desc, boolean done, View.OnClickListener action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#161616"));
        card.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 16;
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView step = new TextView(this);
        step.setText(stepNum);
        step.setTextSize(11);
        step.setTextColor(Color.parseColor("#888888"));
        step.setPadding(0, 0, 12, 0);
        row.addView(step);

        TextView status = new TextView(this);
        status.setText(done ? "✅" : "⏳");
        status.setTextSize(16);
        status.setPadding(0, 0, 10, 0);
        row.addView(status);

        TextView titleV = new TextView(this);
        titleV.setText(stepTitle);
        titleV.setTextSize(15);
        titleV.setTextColor(done ? Color.parseColor("#00e676") : Color.WHITE);
        titleV.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(titleV);
        card.addView(row);

        TextView descV = new TextView(this);
        descV.setText(desc);
        descV.setTextSize(12);
        descV.setTextColor(Color.parseColor("#888888"));
        descV.setPadding(0, 8, 0, 16);
        descV.setLineSpacing(4, 1);
        card.addView(descV);

        if (!done) {
            Button btn = new Button(this);
            btn.setText("GRANT PERMISSION →");
            btn.setBackgroundColor(Color.parseColor("#1a3a2a"));
            btn.setTextColor(Color.parseColor("#00e676"));
            btn.setTextSize(13);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            btn.setPadding(20, 12, 20, 12);
            btn.setOnClickListener(action);
            card.addView(btn);
        } else if (stepNum.equals("Step 3")) {
            Button btn = new Button(this);
            btn.setText("▶ ACTIVATE NOW");
            btn.setBackgroundColor(Color.parseColor("#00e676"));
            btn.setTextColor(Color.BLACK);
            btn.setTextSize(14);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            btn.setPadding(20, 14, 20, 14);
            btn.setOnClickListener(action);
            card.addView(btn);
        }

        parent.addView(card);
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(),
            "enabled_notification_listeners");
        if (flat == null || TextUtils.isEmpty(flat)) return false;
        ComponentName cn = new ComponentName(this, RideNotificationListener.class);
        for (String name : flat.split(":")) {
            if (cn.equals(ComponentName.unflattenFromString(name))) return true;
        }
        return false;
    }

    private void showToast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }
}
