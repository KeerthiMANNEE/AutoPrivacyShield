package com.example.autoprivacyshield;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button startCaptureBtn, startRecordBtn, stopRecordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startCaptureBtn = findViewById(R.id.startBtn);
        startRecordBtn = findViewById(R.id.btnStartStream);
        stopRecordBtn = findViewById(R.id.btnStopStream);

        // Disable recording buttons initially
        startRecordBtn.setEnabled(false);
        stopRecordBtn.setEnabled(false);

        startCaptureBtn.setOnClickListener(v -> requestScreenCapture());

        startRecordBtn.setOnClickListener(v -> {
            Intent startIntent = new Intent(this, ScreenCaptureService.class);
            startIntent.setAction("START_STREAM");
            startService(startIntent);

            startRecordBtn.setEnabled(false);
            stopRecordBtn.setEnabled(true);
            Toast.makeText(this, "Screen recording started", Toast.LENGTH_SHORT).show();
        });

        stopRecordBtn.setOnClickListener(v -> {
            Intent stopIntent = new Intent(this, ScreenCaptureService.class);
            stopIntent.setAction("STOP_STREAM");
            startService(stopIntent);

            startRecordBtn.setEnabled(true);
            stopRecordBtn.setEnabled(false);
            Toast.makeText(this, "Screen recording stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void requestScreenCapture() {
        MediaProjectionManager pm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (pm == null) {
            Toast.makeText(this, "MediaProjection not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = pm.createScreenCaptureIntent();
        screenCaptureLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> screenCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent setupIntent = new Intent(this, ScreenCaptureService.class);
                    setupIntent.setAction("SETUP_PROJECTION");
                    setupIntent.putExtra("resultCode", result.getResultCode());
                    setupIntent.putExtra("data", result.getData());
                    startService(setupIntent);

                    Toast.makeText(this, "Screen capture permission granted", Toast.LENGTH_SHORT).show();

                    startCaptureBtn.setEnabled(false);
                    startRecordBtn.setEnabled(true);
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );
}
