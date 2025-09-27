package com.example.autoprivacyshield;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;
import android.view.Display;
import android.content.Context;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final String TAG = "ScreenCaptureService";

    private MediaProjection mediaProjection;
    private VirtualDisplay recordingVirtualDisplay;
    private VirtualDisplay maskVirtualDisplay;
    private MediaRecorder mediaRecorder;
    private ImageReader imageReader;
    private Handler handler;

    private int screenWidth = 1280;
    private int screenHeight = 720;
    private int densityDpi;

    private boolean isRecording = false;
    private boolean isProjectionReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");

        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, createNotification());
        }

        handler = new Handler(Looper.getMainLooper());

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
            if (dm != null && dm.getDisplay(Display.DEFAULT_DISPLAY) != null) {
                dm.getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(metrics);
                screenWidth = metrics.widthPixels;
                screenHeight = metrics.heightPixels;
                densityDpi = metrics.densityDpi;
            }
            if (screenWidth > 1920) {
                screenHeight = (int) (screenHeight * (1920.0 / screenWidth));
                screenWidth = 1920;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get screen metrics: " + e.getMessage());
            screenWidth = 1280;
            screenHeight = 720;
            densityDpi = 320;
        }

        Log.i(TAG, "Using resolution: " + screenWidth + "x" + screenHeight + " dpi: " + densityDpi);

        DetectionHandler.initialize(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        Log.i(TAG, "Received action: " + action);

        switch (action) {
            case "SETUP_PROJECTION":
                int resultCode = intent.getIntExtra("resultCode", -1);
                Intent data = intent.getParcelableExtra("data");
                if (resultCode == Activity.RESULT_OK && data != null) {
                    setupMediaProjection(resultCode, data);
                } else {
                    showToast("Failed to get screen capture permission");
                }
                break;

            case "START_STREAM":
                startScreenRecording();
                break;

            case "STOP_STREAM":
                stopScreenRecording();
                break;

            default:
                Log.w(TAG, "Unknown action: " + action);
        }

        return START_STICKY;
    }

    private void setupMediaProjection(int resultCode, Intent data) {
        MediaProjectionManager pm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (pm == null) {
            showToast("MediaProjectionManager unavailable");
            return;
        }

        mediaProjection = pm.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            showToast("MediaProjection is null");
            return;
        }

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.i(TAG, "MediaProjection stopped");
                stopScreenRecording();
                cleanup();
                showToast("Screen capture stopped");
            }
        }, handler);

        initImageReaderAndMaskDisplay();

        isProjectionReady = true;
        showToast("Screen capture ready. Start stream to record.");
    }

    private void initImageReaderAndMaskDisplay() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image img = null;
            try {
                img = reader.acquireLatestImage();
                if (img == null) return;

                Bitmap rawBitmap = ImageUtils.imageToBitmap(img);
                img.close();

                if (rawBitmap != null) {
                    DetectionHandler.processBitmap(rawBitmap, (processedBitmap, sensitiveAreas) -> {
                        Log.i(TAG, "Detected " + sensitiveAreas.length + " sensitive regions");
                        // Optionally: send broadcast or update UI here with processedBitmap
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error on image available: ", e);
                if (img != null) img.close();
            }
        }, handler);

        maskVirtualDisplay = mediaProjection.createVirtualDisplay(
                "PrivacyMaskDisplay",
                screenWidth,
                screenHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler
        );

        Log.i(TAG, "Privacy mask virtual display created");
    }

    private void startScreenRecording() {
        if (!isProjectionReady || mediaProjection == null) {
            showToast("Please setup screen capture first");
            return;
        }
        if (isRecording) {
            showToast("Recording in progress");
            return;
        }
        try {
            initRecorder();
            createRecordingVirtualDisplay();
            mediaRecorder.start();
            isRecording = true;
            showToast("Recording started. Saved in Movies folder.");
            updateNotification("Recording screen...");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage(), e);
            showToast("Failed to start recording: " + e.getMessage());
            cleanupRecorder();
        }
    }

    private void initRecorder() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (!storageDir.exists()) storageDir.mkdirs();
        File file = new File(storageDir, "ScreenRecord_" + timestamp + ".mp4");

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(4000000);
        mediaRecorder.setVideoFrameRate(24);
        mediaRecorder.setVideoSize(screenWidth, screenHeight);
        mediaRecorder.setOutputFile(file.getAbsolutePath());
        mediaRecorder.prepare();

        Log.i(TAG, "Recording to: " + file.getAbsolutePath());
    }

    private void createRecordingVirtualDisplay() {
        if (mediaProjection == null || mediaRecorder == null) {
            throw new IllegalStateException("MediaProjection or MediaRecorder is null");
        }
        recordingVirtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecordingDisplay",
                screenWidth,
                screenHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null,
                handler
        );
        if (recordingVirtualDisplay == null) {
            throw new RuntimeException("Failed to create recording virtual display");
        }
        Log.i(TAG, "Recording virtual display created");
    }

    private void stopScreenRecording() {
        if (!isRecording) {
            showToast("No recording in progress");
            return;
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            isRecording = false;
            showToast("Recording stopped");
            updateNotification("Screen capture ready");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage(), e);
            showToast("Error stopping recording: " + e.getMessage());
        } finally {
            cleanupRecorder();
        }
    }

    private void cleanupRecorder() {
        if (recordingVirtualDisplay != null) {
            recordingVirtualDisplay.release();
            recordingVirtualDisplay = null;
            Log.i(TAG, "Recording virtual display released");
        }
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "MediaRecorder release failed: " + e.getMessage());
            }
            mediaRecorder = null;
            Log.i(TAG, "MediaRecorder released");
        }
    }

    private void cleanup() {
        stopScreenRecording();
        isProjectionReady = false;
        if (maskVirtualDisplay != null) {
            maskVirtualDisplay.release();
            maskVirtualDisplay = null;
            Log.i(TAG, "Mask virtual display released");
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
            Log.i(TAG, "ImageReader closed");
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            Log.i(TAG, "MediaProjection stopped");
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AutoPrivacyShield")
                .setContentText("Screen capture service running")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Screen Capture", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Service for screen recording and privacy masking");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("AutoPrivacyShield")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setOngoing(true)
                    .build();
            manager.notify(1, notification);
        }
    }

    private void showToast(String msg) {
        handler.post(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not bound
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
        cleanup();
    }
}
