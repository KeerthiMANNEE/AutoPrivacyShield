package com.example.autoprivacyshield;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.Image.Plane;
import java.nio.ByteBuffer;

public class CaptureUtils {

    public static Bitmap imageToBitmap(Image image) {
        if (image == null) return null;

        Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height);
    }
}
