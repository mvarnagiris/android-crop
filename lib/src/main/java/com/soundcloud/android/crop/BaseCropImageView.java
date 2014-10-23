/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public abstract class BaseCropImageView extends ImageView implements PreviewImage.PreviewImageListener {
    private static final float ZOOM_SCALE_STEP = 1.25f;

    private final Handler handler = new Handler();

    protected PreviewImage previewImage;

    public BaseCropImageView(Context context) {
        super(context);
        init();
    }

    public BaseCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseCropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onPreviewImageChanged(Matrix matrix) {
        setImageMatrix(matrix);
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        throw new UnsupportedOperationException("Use setPreviewImage(PreviewImage) instead.");
    }

    public void setPreviewImage(PreviewImage previewImage) {
        if (previewImage == null) {
            recycleOldBitmap(null);
            this.previewImage = null;
            return;
        }

        final Bitmap newBitmap = previewImage.getBitmap();
        super.setImageBitmap(newBitmap);

        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setDither(true);
        }

        recycleOldBitmap(newBitmap);
        if (this.previewImage != null) {
            this.previewImage.setListener(null);
        }
        this.previewImage = previewImage;
        this.previewImage.setListener(this);

        onPreviewImageChanged(previewImage.getPreviewMatrix());
    }

    public void clear() {
        setPreviewImage(null);
    }

    protected void center(boolean horizontal, boolean vertical) {
        if (previewImage != null) {
            previewImage.center(horizontal, vertical);
        }
    }

    protected void zoomTo(float scale, float centerX, float centerY) {
        if (previewImage != null) {
            previewImage.zoomTo(scale, centerX, centerY);
        }
    }

    protected void zoomTo(final float scale, final float centerX, final float centerY, final float durationMs) {
        if (previewImage == null) {
            return;
        }

        final float incrementPerMs = (scale - previewImage.getScale()) / durationMs;
        final float oldScale = previewImage.getScale();
        final long startTime = System.currentTimeMillis();

        handler.post(new Runnable() {
            public void run() {
                long now = System.currentTimeMillis();
                float currentMs = Math.min(durationMs, now - startTime);
                float target = oldScale + (incrementPerMs * currentMs);
                zoomTo(target, centerX, centerY);

                if (currentMs < durationMs) {
                    handler.post(this);
                }
            }
        });
    }

    protected void zoomIn() {
        zoomIn(ZOOM_SCALE_STEP);
    }

    protected void zoomIn(float rate) {
        if (previewImage != null) {
            previewImage.zoomIn(rate);
        }
    }

    protected void zoomOut() {
        zoomOut(ZOOM_SCALE_STEP);
    }

    protected void zoomOut(float rate) {
        if (previewImage != null) {
            previewImage.zoomOut(rate);
        }
    }

    protected void panBy(float dx, float dy) {
        if (previewImage != null) {
            previewImage.panBy(dx, dy);
        }
    }

    private void init() {
        setScaleType(ImageView.ScaleType.MATRIX);
    }

    private void recycleOldBitmap(Bitmap newBitmap) {
        if (previewImage == null) {
            return;
        }

        final Bitmap oldBitmap = previewImage.getBitmap();
        if (oldBitmap == null) {
            return;
        }

        if (oldBitmap != newBitmap) {
            oldBitmap.recycle();
        }
    }
}
