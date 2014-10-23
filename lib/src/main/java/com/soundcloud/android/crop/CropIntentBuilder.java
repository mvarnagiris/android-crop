package com.soundcloud.android.crop;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

public class CropIntentBuilder {
    private final Intent cropIntent;

    public CropIntentBuilder(Context context, Uri sourceUri, Uri outputUri) {
        if (context == null) {
            throw new NullPointerException("Context cannot be null.");
        }

        if (sourceUri == null) {
            throw new NullPointerException("Source uri cannot be null.");
        }

        if (outputUri == null) {
            throw new NullPointerException("Output uri cannot be null.");
        }

        cropIntent = new Intent(context, CropImageActivity.class);
        cropIntent.setData(sourceUri);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
    }

    /**
     * Set fixed aspect ratio for crop area
     *
     * @param x Aspect X
     * @param y Aspect Y
     */
    public CropIntentBuilder withAspect(int x, int y) {
        cropIntent.putExtra(Extra.ASPECT_X, x);
        cropIntent.putExtra(Extra.ASPECT_Y, y);
        return this;
    }

    /**
     * Crop area with fixed 1:1 aspect ratio
     */
    public CropIntentBuilder asSquare() {
        withAspect(1, 1);
        return this;
    }

    /**
     * Set maximum output size
     *
     * @param width  Max width
     * @param height Max height
     */
    public CropIntentBuilder withMaxSize(int width, int height) {
        cropIntent.putExtra(Extra.MAX_X, width);
        cropIntent.putExtra(Extra.MAX_Y, height);
        return this;
    }

    /**
     * Builds intent for crop activity.
     */
    public Intent build() {
        return cropIntent;
    }

    public static interface Extra {
        String ASPECT_X = "aspect_x";
        String ASPECT_Y = "aspect_y";
        String MAX_X = "max_x";
        String MAX_Y = "max_y";
    }
}
