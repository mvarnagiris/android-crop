package com.soundcloud.android.crop;

import android.content.Intent;

class CropConfig {
    private final int aspectX;
    private final int aspectY;
    private final int maxX;
    private final int maxY;

    private CropConfig(int aspectX, int aspectY, int maxX, int maxY) {
        this.aspectX = aspectX;
        this.aspectY = aspectY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public static CropConfig from(Intent intent) {
        if (intent == null) {
            throw new NullPointerException("Intent cannot be null.");
        }

        if (intent.getExtras() == null) {
            return new CropConfig(0, 0, 0, 0);
        }

        final int aspectX = intent.getIntExtra(CropIntentBuilder.Extra.ASPECT_X, 0);
        final int aspectY = intent.getIntExtra(CropIntentBuilder.Extra.ASPECT_Y, 0);
        final int maxX = intent.getIntExtra(CropIntentBuilder.Extra.MAX_X, 0);
        final int maxY = intent.getIntExtra(CropIntentBuilder.Extra.MAX_Y, 0);

        return new CropConfig(aspectX, aspectY, maxX, maxY);
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
