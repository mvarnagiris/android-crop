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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public abstract class BaseCropImageView extends ImageView {
    private static final float ZOOM_SCALE_STEP = 1.25f;

    protected final RotateBitmap previewBitmap = new RotateBitmap(null, 0);

    private final Handler handler = new Handler();
    private final Matrix previewMatrix = new Matrix();
    private final Matrix transformationMatrix = new Matrix();
    private final Matrix baseMatrix = new Matrix();
    private final float[] matrixBuffer = new float[9];

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
    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap, 0);
    }

    public void setImageBitmap(RotateBitmap bitmap) {
        setImageBitmap(bitmap.getBitmap(), bitmap.getRotation());
    }

    public void clear() {
        setImageBitmap(null, 0);
    }

    public Matrix getUnrotatedPreviewMatrix() {
        final Matrix unrotated = calculateBaseMatrix(previewBitmap, false, null);
        unrotated.postConcat(transformationMatrix);
        return unrotated;
    }

    protected float getScale() {
        return getScale(transformationMatrix);
    }

    protected void center(boolean horizontal, boolean vertical) {
        final Bitmap bitmap = previewBitmap.getBitmap();
        if (bitmap == null) {
            return;
        }

        final Matrix matrix = getPreviewMatrix();
        final RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        matrix.mapRect(rect);

        float height = rect.height();
        float width = rect.width();

        float deltaX = 0;
        float deltaY = 0;

        if (vertical) {
            int viewHeight = getHeight();
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = getHeight() - rect.bottom;
            }
        }

        if (horizontal) {
            int viewWidth = getWidth();
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
            }
        }

        postTranslate(deltaX, deltaY);
        setImageMatrix(getPreviewMatrix());
    }

    protected void zoomTo(float scale) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        zoomTo(scale, cx, cy);
    }

    protected void zoomTo(float scale, float centerX, float centerY) {
        final float maxZoom = getMaxZoom();
        if (scale > maxZoom) {
            scale = maxZoom;
        }

        float oldScale = getScale();
        float deltaScale = scale / oldScale;

        transformationMatrix.postScale(deltaScale, deltaScale, centerX, centerY);
        setImageMatrix(getPreviewMatrix());
        center(true, true);
    }

    protected void zoomTo(final float scale, final float centerX, final float centerY, final float durationMs) {
        final float incrementPerMs = (scale - getScale()) / durationMs;
        final float oldScale = getScale();
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
        if (previewBitmap.getBitmap() == null) {
            return;
        }

        if (getScale() >= getMaxZoom()) {
            return; // Don't let the user zoom into the molecular level
        }

        float cx = getMeasuredWidth() / 2f;
        float cy = getMeasuredHeight() / 2f;

        transformationMatrix.postScale(rate, rate, cx, cy);
        setImageMatrix(getPreviewMatrix());
    }

    protected void zoomOut() {
        zoomOut(ZOOM_SCALE_STEP);
    }

    protected void zoomOut(float rate) {
        if (previewBitmap.getBitmap() == null) {
            return;
        }

        float cx = getMeasuredWidth() / 2f;
        float cy = getMeasuredHeight() / 2f;

        // Zoom out to at most 1x
        Matrix tmp = new Matrix(transformationMatrix);
        tmp.postScale(1f / rate, 1f / rate, cx, cy);

        if (getScale(tmp) < 1f) {
            transformationMatrix.setScale(1f, 1f, cx, cy);
        } else {
            transformationMatrix.postScale(1f / rate, 1f / rate, cx, cy);
        }

        setImageMatrix(getPreviewMatrix());
        center(true, true);
    }

    protected void panBy(float dx, float dy) {
        postTranslate(dx, dy);
        setImageMatrix(getPreviewMatrix());
    }

    protected void postTranslate(float dx, float dy) {
        transformationMatrix.postTranslate(dx, dy);
    }

    private void init() {
        setScaleType(ImageView.ScaleType.MATRIX);
    }

    private void setImageBitmap(Bitmap bitmap, int rotation) {
        super.setImageBitmap(bitmap);

        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setDither(true);
        }

        // Update preview bitmap
        Bitmap old = previewBitmap.getBitmap();
        previewBitmap.setBitmap(bitmap);
        previewBitmap.setRotation(rotation);
        if (old != null && old != bitmap) {
            old.recycle();
        }

        // Update matrices
        transformationMatrix.reset();
        if (bitmap == null) {
            baseMatrix.reset();
        } else {
            calculateBaseMatrix(previewBitmap, true, baseMatrix);
        }
        setImageMatrix(getPreviewMatrix());
    }

    private Matrix calculateBaseMatrix(RotateBitmap bitmap, boolean includeRotation, Matrix matrix) {
        if (matrix == null) {
            matrix = new Matrix();
        }
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float w = bitmap.getWidth();
        float h = bitmap.getHeight();
        matrix.reset();

        // We limit up-scaling to 3x otherwise the result may look bad if it's a small icon
        float widthScale = Math.min(viewWidth / w, 3.0f);
        float heightScale = Math.min(viewHeight / h, 3.0f);
        float scale = Math.min(widthScale, heightScale);

        if (includeRotation) {
            matrix.postConcat(bitmap.getRotateMatrix());
        }
        matrix.postScale(scale, scale);
        matrix.postTranslate((viewWidth - w * scale) / 2F, (viewHeight - h * scale) / 2F);
        return matrix;
    }

    private Matrix getPreviewMatrix() {
        previewMatrix.set(baseMatrix);
        previewMatrix.postConcat(transformationMatrix);
        return previewMatrix;
    }

    private float getMaxZoom() {
        if (previewBitmap.getBitmap() == null) {
            return 1f;
        }

        float fw = (float) previewBitmap.getWidth() / getMeasuredWidth();
        float fh = (float) previewBitmap.getHeight() / getMeasuredHeight();
        return Math.max(fw, fh) * 4; // 400%
    }

    private float getScale(Matrix matrix) {
        return getValueFromMatrix(matrix, Matrix.MSCALE_X);
    }

    private float getValueFromMatrix(Matrix matrix, int whichValue) {
        matrix.getValues(matrixBuffer);
        return matrixBuffer[whichValue];
    }
}
