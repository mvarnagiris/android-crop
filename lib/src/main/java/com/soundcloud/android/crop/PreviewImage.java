package com.soundcloud.android.crop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class PreviewImage {
    private final RotateBitmap bitmap;

    public PreviewImage(PreviewSize previewSize, SourceImage sourceImage) throws Exception {
        final int sampleSize = calculateSampleSize(previewSize, sourceImage);

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        final Bitmap tempBitmap = sourceImage.decodeBitmap(options);
        final int exifRotation = sourceImage.getExifRotation();

        bitmap = new RotateBitmap(tempBitmap, exifRotation);
    }

    public RotateBitmap getBitmap() {
        return bitmap;
    }

    public int getWidth() {
        return bitmap.getWidth();
    }

    public int getHeight() {
        return bitmap.getHeight();
    }

    private int calculateSampleSize(PreviewSize previewSize, SourceImage sourceImage) throws Exception {
        // Decode bitmap bounds
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        sourceImage.decodeBitmap(options);

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > previewSize.getHeight() || width > previewSize.getWidth()) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both height
            // and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > previewSize.getHeight() && (halfWidth / inSampleSize) > previewSize.getWidth()) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
