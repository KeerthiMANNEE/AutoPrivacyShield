package com.example.autoprivacyshield;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private Handler handler;
    private int screenDensity;
    private int screenWidth;
    private int screenHeight;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AutoPrivacyShield")
                .setContentText("Screen capture is running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        handler = new Handler(Looper.getMainLooper());

        // Get screen metrics
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        screenDensity = metrics.densityDpi;
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager != null && data != null) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            initVirtualDisplay();
        }

        return START_STICKY;
    }

    private void initVirtualDisplay() {
        if (mediaProjection == null) return;

        // Register callback to manage MediaProjection lifecycle
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                if (imageReader != null) {
                    imageReader.close();
                    imageReader = null;
                }
                mediaProjection = null;
                stopSelf();
            }
        }, handler);

        // Create ImageReader to capture screen frames
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

        mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                0,
                imageReader.getSurface(),
                null,
                handler);

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    // Convert Image to Bitmap
                    Bitmap bitmap = ImageUtils.imageToBitmap(image);
                    image.close();

                    if (bitmap != null) {
                        // Pass bitmap to detection handler (stub for now)
                        DetectionHandler.processBitmap(bitmap);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) image.close();
            }
        }, handler);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Capture Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
