package com.example.autoprivacyshield;

import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NotificationService extends NotificationListenerService {
    public static final String ACTION_NEW_NOTIFICATION = "com.example.autoprivacyshield.NEW_NOTIFICATION";
    public static final String EXTRA_NOTIFICATION_TEXT = "notification_text";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");

        Log.d("NotificationService", "Notification from: " + pkg + " | " + title + " | " + text);

        Intent intent = new Intent(ACTION_NEW_NOTIFICATION);
        String combinedText = (title == null ? "" : title + ": ") + (text == null ? "" : text);
        intent.putExtra(EXTRA_NOTIFICATION_TEXT, combinedText);

        // Send local broadcast instead of system-wide broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("NotificationService", "Notification removed: " + sbn.getPackageName());
    }
}
