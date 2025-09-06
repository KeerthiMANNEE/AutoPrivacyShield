package com.example.autoprivacyshield;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DetectionHandler {
    private static final String TAG = "DetectionHandler";
    private static DetectionUtils detectionUtils;

    public static void initialize(Context context) {
        if (detectionUtils == null) {
            detectionUtils = new DetectionUtils(context);
            Log.d(TAG, "DetectionHandler initialized with Team B functionality");
        }
    }

    public static void processBitmap(Bitmap bitmap, ProcessingCallback callback) {
        if (detectionUtils == null) {
            Log.e(TAG, "DetectionHandler not initialized!");
            callback.onProcessingComplete(bitmap, new Rect[0]);
            return;
        }

        Log.d(TAG, "Processing bitmap of size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        detectionUtils.detectSensitiveRegions(bitmap, results -> {
            List<Rect> sensitiveBoxes = new ArrayList<>();

            for (DetectResult result : results) {
                if (result.isSensitive()) {
                    sensitiveBoxes.add(result.getBoundingBox());
                    Log.d(TAG, "Found sensitive " + result.getType() + ": " + result.getText());
                }
            }

            Rect[] sensitiveAreas = sensitiveBoxes.toArray(new Rect[0]);
            Bitmap maskedBitmap = MaskingUtils.blurRegions(bitmap, sensitiveAreas);
            callback.onProcessingComplete(maskedBitmap, sensitiveAreas);
        });
    }

    public interface ProcessingCallback {
        void onProcessingComplete(Bitmap processedBitmap, Rect[] sensitiveAreas);
    }
}
