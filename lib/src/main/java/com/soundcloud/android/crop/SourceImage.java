package com.soundcloud.android.crop;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;

import java.io.InputStream;

class SourceImage {
    private final Uri uri;
    private final int exifRotation;
    private final int width;
    private final int height;

    public SourceImage(ContentResolver contentResolver, Intent intent) throws Exception {
        uri = intent.getData();

        if (uri == null) {
            throw new IllegalArgumentException("Intent must have source image uri.");
        }

        exifRotation = CropUtil.getExifRotation(CropUtil.getFromMediaUri(contentResolver, uri));

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        decodeBitmap(contentResolver, options);
        width = options.outWidth;
        height = options.outHeight;
    }

    private SourceImage(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        exifRotation = in.readInt();
        width = in.readInt();
        height = in.readInt();
    }

    public int getExifRotation() {
        return exifRotation;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Bitmap decodeBitmap(ContentResolver contentResolver, BitmapFactory.Options options) throws Exception {
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
