package com.soundcloud.android.crop;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

class CropConfig implements Parcelable {
    public static final Parcelable.Creator<CropConfig> CREATOR = new Parcelable.Creator<CropConfig>() {
        public CropConfig createFromParcel(Parcel in) {
            return new CropConfig(in);
        }

        public CropConfig[] newArray(int size) {
            return new CropConfig[size];
        }
    };

    private final Uri outputUri;
    private final int aspectX;
    private final int aspectY;
    private final int maxX;
    private final int maxY;

    private CropConfig(Uri outputUri, int aspectX, int aspectY, int maxX, int maxY) {
        this.outputUri = outputUri;
        this.aspectX = aspectX;
        this.aspectY = aspectY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    private CropConfig(Parcel in) {
        this.outputUri = in.readParcelable(Uri.class.getClassLoader());
        this.aspectX = in.readInt();
        this.aspectY = in.readInt();
        this.maxX = in.readInt();
        this.maxY = in.readInt();
    }

    public static CropConfig from(Intent intent) {
        if (intent == null) {
            throw new NullPointerException("Intent cannot be null.");
        }

        if (intent.getExtras() == null) {
            throw new NullPointerException("Intent must contain output uri.");
        }

        final Uri outputUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        final int aspectX = intent.getIntExtra(CropIntentBuilder.Extra.ASPECT_X, 0);
        final int aspectY = intent.getIntExtra(CropIntentBuilder.Extra.ASPECT_Y, 0);
        final int maxX = intent.getIntExtra(CropIntentBuilder.Extra.MAX_X, 0);
        final int maxY = intent.getIntExtra(CropIntentBuilder.Extra.MAX_Y, 0);

        return new CropConfig(outputUri, aspectX, aspectY, maxX, maxY);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(outputUri, flags);
        dest.writeInt(aspectX);
        dest.writeInt(aspectY);
        dest.writeInt(maxX);
        dest.writeInt(maxY);
    }

    public Uri getOutputUri() {
        return outputUri;
    }

    public int getAspectX() {
        return aspectX;
    }

    public int getAspectY() {
        return aspectY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }
}
