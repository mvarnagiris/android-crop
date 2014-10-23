package com.soundcloud.android.crop;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Crop {
    private Crop() {
    }

    public static CropIntentBuilder createIntentBuilder(Context context, Uri sourceUri, Uri outputUri) {
        return new CropIntentBuilder(context, sourceUri, outputUri);
    }

    /**
     * Retrieve URI for cropped image, as set in the Intent builder
     *
     * @param result Output Image URI
     */
    public static Uri getOutput(Intent result) {
        return result.getData();
    }

    /**
     * Retrieve error that caused crop to fail
     *
     * @param result Result Intent
     * @return Throwable handled in CropImageActivity
     */
    public static Throwable getError(Intent result) {
        return (Throwable) result.getSerializableExtra(BaseCropActivity.RESULT_EXTRA_ERROR);
    }
}
