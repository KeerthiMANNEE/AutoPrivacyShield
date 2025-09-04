package com.example.autoprivacyshield;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

public class MediaProjectionHelper {

    public static MediaProjection getMediaProjection(Context context, int resultCode, Intent data) {
        MediaProjectionManager manager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager != null) {
            return manager.getMediaProjection(resultCode, data);
        }
        return null;
    }
}

