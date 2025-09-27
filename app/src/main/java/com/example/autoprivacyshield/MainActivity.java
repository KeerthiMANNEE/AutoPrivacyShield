package com.example.autoprivacyshield;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ImageView imageView;
    private TextView notificationTextView;
    private Button startCaptureBtn, startStreamBtn, stopStreamBtn;
    private Handler handler;

    private ScreenCaptureService captureService;
    private boolean isBound = false;

    private NotificationBroadcastReceiver notificationReceiver;

    private final String rtmpUrl = "rtmp://a.rtmp.youtube.com/live2/YOUR_STREAM_KEY";  // Replace with your actual YouTube Live stream key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        notificationTextView = findViewById(R.id.notificationTextView);
        startCaptureBtn = findViewById(R.id.startBtn);
        startStreamBtn = findViewById(R.id.startStreamBtn);
        stopStreamBtn = findViewById(R.id.stopStreamBtn);
        handler = new Handler();

        notificationReceiver = new NotificationBroadcastReceiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationReceiver,
                new android.content.IntentFilter(NotificationService.ACTION_NEW_NOTIFICATION)
        );

        startCaptureBtn.setOnClickListener(v -> requestScreenCapture());

        startStreamBtn.setOnClickListener(v -> {
            if (isBound && captureService != null) {
                captureService.startStream(rtmpUrl);
                Toast.makeText(this, "Streaming started", Toast.LENGTH_SHORT).show();
            }
        });

        stopStreamBtn.setOnClickListener(v -> {
            if (isBound && captureService != null) {
                captureService.stopStream();
                Toast.makeText(this, "Streaming stopped", Toast.LENGTH_SHORT).show();
            }
        });

        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        handler.postDelayed(frameUpdateRunnable, 1000);
    }

    private void requestScreenCapture() {
        android.media.projection.MediaProjectionManager projectionManager =
                (android.media.projection.MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager != null) {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            screenCaptureResultLauncher.launch(captureIntent);
        }
    }

    private final ActivityResultLauncher<android.content.Intent> screenCaptureResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                    serviceIntent.putExtra("resultCode", result.getResultCode());
                    serviceIntent.putExtra("data", result.getData());
                    startForegroundService(serviceIntent);

                    Toast.makeText(this, "Privacy protection is now active!", Toast.LENGTH_LONG).show();
                    startCaptureBtn.setText("Privacy Protection Active");
                    startCaptureBtn.setEnabled(false);
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final Runnable frameUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && captureService != null) {
                Bitmap currentFrame = captureService.getCurrentFrame();
                if (currentFrame != null) {
                    imageView.setImageBitmap(currentFrame);
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ScreenCaptureService.LocalBinder localBinder = (ScreenCaptureService.LocalBinder) binder;
            captureService = localBinder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            captureService = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacksAndMessages(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    private class NotificationBroadcastReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (NotificationService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
                String notificationText = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TEXT);
                notificationTextView.setText("Latest: " + notificationText);
            }
        }
    }
}
