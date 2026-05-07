package com.rideacceptor;

import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class RideNotificationListener extends NotificationListenerService {

    // Package names of ride apps
    private static final String PKG_UBER       = "com.ubercab.driver";
    private static final String PKG_RAPIDO     = "com.rapido.passenger"; // captain app
    private static final String PKG_RAPIDO2    = "com.rapido.captain";
    private static final String PKG_OLA        = "com.olacabs.driver";
    private static final String PKG_LOADSHARE  = "com.loadshare.captain";
    private static final String PKG_LOADSHARE2 = "in.loadshare.driver";

    // Keywords that indicate a NEW booking/order
    private static final String[] BOOKING_KEYWORDS = {
        "new trip", "new ride", "trip request", "ride request",
        "new order", "booking request", "new booking",
        "accept", "नई सवारी", "नई बुकिंग", "नया ऑर्डर",
        "new delivery", "pickup request"
    };

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();

        if (!isRideApp(pkg)) return;

        android.app.Notification notif = sbn.getNotification();
        if (notif == null) return;

        // Extract notification text
        String title = "";
        String text = "";
        if (notif.extras != null) {
            CharSequence t = notif.extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
            CharSequence b = notif.extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
            if (t != null) title = t.toString();
            if (b != null) text = b.toString();
        }

        String combined = (title + " " + text).toLowerCase();

        // Only trigger if it looks like a booking (not a promo or status update)
        if (!isBookingNotification(combined, pkg)) return;

        // Launch the overlay
        Intent i = new Intent(this, OverlayService.class);
        i.setAction("SHOW_ALERT");
        i.putExtra("app_name", getAppName(pkg));
        i.putExtra("title", title);
        i.putExtra("text", text);
        i.putExtra("pkg", pkg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(i);
        else
            startService(i);
    }

    private boolean isRideApp(String pkg) {
        return pkg.equals(PKG_UBER) ||
               pkg.equals(PKG_RAPIDO) ||
               pkg.equals(PKG_RAPIDO2) ||
               pkg.equals(PKG_OLA) ||
               pkg.equals(PKG_LOADSHARE) ||
               pkg.equals(PKG_LOADSHARE2);
    }

    private boolean isBookingNotification(String text, String pkg) {
        // Uber: always show when new notification comes from driver app
        if (pkg.equals(PKG_UBER)) {
            // Filter out known non-booking notifications
            if (text.contains("surge") && !text.contains("trip")) return false;
            if (text.contains("promotion") || text.contains("promo")) return false;
            return true; // Uber driver notifications are almost always trips
        }

        // For others, check for booking keywords
        for (String kw : BOOKING_KEYWORDS) {
            if (text.contains(kw)) return true;
        }

        // If it's from rapido/ola/loadshare and no exclusion matched, show it
        // (better to show too many than miss a booking)
        if (text.contains("promotion") || text.contains("promo") ||
            text.contains("offer") || text.contains("cashback") ||
            text.contains("update") || text.contains("arrived")) {
            return false;
        }

        return true; // Default: show overlay for unknown notifications from ride apps
    }

    private String getAppName(String pkg) {
        switch (pkg) {
            case PKG_UBER: return "UBER";
            case PKG_RAPIDO:
            case PKG_RAPIDO2: return "RAPIDO";
            case PKG_OLA: return "OLA";
            case PKG_LOADSHARE:
            case PKG_LOADSHARE2: return "LOADSHARE";
            default: return "RIDE APP";
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Optionally dismiss overlay when notification is cancelled
        if (sbn != null && isRideApp(sbn.getPackageName())) {
            Intent i = new Intent(this, OverlayService.class);
            i.setAction("DISMISS");
            startService(i);
        }
    }
}
