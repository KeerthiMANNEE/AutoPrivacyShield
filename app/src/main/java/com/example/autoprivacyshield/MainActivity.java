package com.example.autoprivacyshield;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.tensorflow.lite.Interpreter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Team A UI Components
    private ImageView imageView;
    private TextView notificationTextView;
    private Button startBtn;
    private Handler handler;
    private NotificationBroadcastReceiver notificationReceiver;

    // Team B UI Components
    private Button btnFaceOcr, btnOcrOnly, btnFaceOnly;
    private Interpreter yoloInterpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Team A UI components
        initTeamAComponents();

        // Initialize Team B UI components
        initTeamBComponents();

        // Initialize detection system
        DetectionHandler.initialize(this);

        Log.d(TAG, "AutoPrivacyShield initialized with full Team A + Team B integration");
    }

    private void initTeamAComponents() {
        imageView = findViewById(R.id.imageView);
        notificationTextView = findViewById(R.id.notificationTextView);
        startBtn = findViewById(R.id.startBtn);
        handler = new Handler();

        notificationReceiver = new NotificationBroadcastReceiver();
        IntentFilter filter = new IntentFilter(NotificationService.ACTION_NEW_NOTIFICATION);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter);

        startBtn.setOnClickListener(v -> requestScreenCapture());

        // Periodically update ImageView with latest frame (to be implemented: bind with service for actual frame)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO: Update imageView with frame from ScreenCaptureService
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void initTeamBComponents() {
        btnFaceOcr = findViewById(R.id.btnFaceOcr);
        btnOcrOnly = findViewById(R.id.btnOcrOnly);
        btnFaceOnly = findViewById(R.id.btnFaceOnly);

        btnFaceOcr.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FaceOcrActivity.class);
            startActivity(intent);
        });

        btnOcrOnly.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OCRActivity.class);
            startActivity(intent);
        });

        btnFaceOnly.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FaceDetectionActivity.class);
            startActivity(intent);
        });

        try {
            YoloV8Helper helper = new YoloV8Helper(this);
            yoloInterpreter = helper.getInterpreter();
            Log.d(TAG, "✅ YOLOv8 model loaded successfully!");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to load YOLOv8 model (model file may be missing)", e);
        }
    }

    private void requestScreenCapture() {
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager != null) {
            Intent intent = projectionManager.createScreenCaptureIntent();
            screenCaptureResultLauncher.launch(intent);
        }
    }

    private final ActivityResultLauncher<Intent> screenCaptureResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                    serviceIntent.putExtra("resultCode", result.getResultCode());
                    serviceIntent.putExtra("data", result.getData());
                    startForegroundService(serviceIntent);

                    Toast.makeText(this, "Privacy protection is now active!", Toast.LENGTH_LONG).show();
                    startBtn.setText("Privacy Protection Active");
                    startBtn.setEnabled(false);
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        handler.removeCallbacksAndMessages(null);
    }

    private class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
                String notificationText = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TEXT);
                notificationTextView.setText("Latest: " + notificationText);
            }
        }
    }
}
