package com.soundcloud.android.crop;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.InputStream;

class SourceImage implements Parcelable {
    public static final Parcelable.Creator<SourceImage> CREATOR = new Parcelable.Creator<SourceImage>() {
        public SourceImage createFromParcel(Parcel in) {
            return new SourceImage(in);
        }

        public SourceImage[] newArray(int size) {
            return new SourceImage[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeInt(exifRotation);
        dest.writeInt(width);
        dest.writeInt(height);
    }

    public Uri getUri() {
        return uri;
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
            throw new Exception("Out of memory");
        } finally {
            CropUtil.closeSilently(is);
        }
    }

    public Bitmap decodeRegion(ContentResolver contentResolver, Rect cropRect, int outWidth, int outHeight) throws Exception {
        InputStream inputStream = null;
        try {
            inputStream = contentResolver.openInputStream(uri);
            final BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inputStream, false);
            final int width = decoder.getWidth();
            final int height = decoder.getHeight();

            if (exifRotation != 0) {
                // Adjust crop area to account for image rotation
                Matrix matrix = new Matrix();
                matrix.setRotate(-exifRotation);

                RectF adjusted = new RectF();
                matrix.mapRect(adjusted, new RectF(cropRect));

                // Adjust to account for origin at 0,0
                adjusted.offset(adjusted.left < 0 ? width : 0, adjusted.top < 0 ? height : 0);
                cropRect = new Rect((int) adjusted.left, (int) adjusted.top, (int) adjusted.right, (int) adjusted.bottom);
            }

            // Try to decode smallest possible region
            final BitmapFactory.Options options = new BitmapFactory.Options();
            final int cropWidth = cropRect.width();
            final int cropHeight = cropRect.height();
            options.inSampleSize = 1;
            while (cropWidth / (options.inSampleSize + 1) > outWidth && cropHeight / (options.inSampleSize + 1) > outHeight) {
                options.inSampleSize++;
            }

            return decoder.decodeRegion(cropRect, options);
        } catch (OutOfMemoryError e) {
            throw new Exception("Out of memory");
        } finally {
            CropUtil.closeSilently(inputStream);
        }
    }
}
