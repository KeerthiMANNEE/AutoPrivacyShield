package com.example.autoprivacyshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView notificationTextView;
    private Handler handler;
    private ScreenCaptureService captureService; // Bind or reference your service instance accordingly
    private NotificationBroadcastReceiver notificationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        notificationTextView = findViewById(R.id.notificationTextView);
        handler = new Handler();

        notificationReceiver = new NotificationBroadcastReceiver();
        IntentFilter filter = new IntentFilter(NotificationService.ACTION_NEW_NOTIFICATION);
        // Register receiver with LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter);

        // Periodic update of ImageView with latest captured frame
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Bitmap frame = null;
                if (captureService != null) {
                    frame = captureService.getCurrentFrame();
                }
                if (frame != null) {
                    imageView.setImageBitmap(frame);
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);

        // TODO: Bind or obtain your ScreenCaptureService instance to set captureService
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver with LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    private class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
                String notificationText = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TEXT);
                notificationTextView.setText(notificationText);
            }
        }
    }
}
