package com.soundcloud.android.crop;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

class SourceImage {
    private final ContentResolver contentResolver;
    private final Uri uri;
    private final Integer exifRotation;

    public SourceImage(ContentResolver contentResolver, Intent intent) {
        if (contentResolver == null) {
            throw new NullPointerException("Content resolver cannot be null.");
        }

        this.contentResolver = contentResolver;

        if (intent == null) {
            throw new NullPointerException("Intent cannot be null.");
        }

        uri = intent.getData();

        if (uri == null) {
            throw new IllegalArgumentException("Intent must have source image uri.");
        }

        exifRotation = CropUtil.getExifRotation(CropUtil.getFromMediaUri(contentResolver, uri));
    }

    public int getExifRotation() {
        return exifRotation;
    }

    public Bitmap decodeBitmap(BitmapFactory.Options options) throws Exception {
        InputStream is = null;
        try {
            is = contentResolver.openInputStream(uri);
            return BitmapFactory.decodeStream(is, null, options);
        } catch (OutOfMemoryError e) {
            throw new Exception(e.getMessage());
        } finally {
            CropUtil.closeSilently(is);
        }
    }
}
