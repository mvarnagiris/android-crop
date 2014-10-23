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
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

@SuppressWarnings("UnusedDeclaration")
public class CropImageView extends ImageView implements PreviewImage.PreviewImageListener {
    private static final float ZOOM_SCALE_STEP = 1.25f;

    private final Handler handler = new Handler();

    private HighlightView highlightView;

    private PreviewImage previewImage;
    private float lastX;
    private float lastY;
    private int motionEdge;

    public CropImageView(Context context) {
        super(context);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (previewImage != null && highlightView != null) {
            updateHighlightViewMatrix();
            if (highlightView.hasFocus()) {
                centerBasedOnHighlightView(highlightView);
            }
        }
    }

    @Override
    protected void onDraw(@SuppressWarnings("NullableProblems") Canvas canvas) {
        super.onDraw(canvas);
        if (highlightView != null) {
            highlightView.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (highlightView != null) {
                    int edge = highlightView.getHit(event.getX(), event.getY());
                    if (edge != HighlightView.GROW_NONE) {
                        motionEdge = edge;
                        lastX = event.getX();
                        lastY = event.getY();
                        highlightView.setMode((edge == HighlightView.MOVE) ? HighlightView.ModifyMode.Move : HighlightView.ModifyMode.Grow);
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (highlightView != null) {
                    centerBasedOnHighlightView(highlightView);
                    highlightView.setMode(HighlightView.ModifyMode.None);
                }
                center(true, true);
                break;

            case MotionEvent.ACTION_MOVE:
                if (highlightView != null) {
                    highlightView.handleMotion(motionEdge, event.getX()
                            - lastX, event.getY() - lastY);
                    lastX = event.getX();
                    lastY = event.getY();
                    ensureVisible(highlightView);
                }

                // If we're not zoomed then there's no point in even allowing the user to move the
                // image around. This call to center puts it back to the normalized location.
                if (previewImage != null && previewImage.getScale() == 1F) {
                    center(true, true);
                }
                break;
        }

        return true;
    }

    @Override
    public void onPreviewImageChanged(Matrix matrix) {
        setImageMatrix(matrix);
        updateHighlightViewMatrix();
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

    public HighlightView getHighlightView() {
        return highlightView;
    }

    public void setHighlightView(HighlightView highlightView) {
        this.highlightView = highlightView;
        invalidate();
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

    /**
     * If the cropping rectangle's size changed significantly, change the view's center and scale
     * according to the cropping rectangle.
     */
    private void centerBasedOnHighlightView(HighlightView hv) {
        final Rect drawRect = hv.drawRect;

        final float width = drawRect.width();
        final float height = drawRect.height();

        final float z1 = getMeasuredWidth() / width * 0.6f;
        final float z2 = getMeasuredHeight() / height * 0.6f;

        float zoom = Math.min(z1, z2);
        zoom = zoom * previewImage.getScale();
        zoom = Math.max(1f, zoom);

        if ((Math.abs(zoom - previewImage.getScale()) / zoom) > .1) {
            float[] coordinates = new float[]{hv.cropRect.centerX(), hv.cropRect.centerY()};
            previewImage.getUnrotatedPreviewMatrix(null).mapPoints(coordinates);
            zoomTo(zoom, coordinates[0], coordinates[1], 300f);
        }

        ensureVisible(hv);
    }

    /**
     * Pan the displayed image to make sure the cropping rectangle is visible.
     */
    private void ensureVisible(HighlightView hv) {
        final Rect r = hv.drawRect;

        final int panDeltaX1 = Math.max(0, getLeft() - r.left);
        final int panDeltaX2 = Math.min(0, getRight() - r.right);

        final int panDeltaY1 = Math.max(0, getTop() - r.top);
        final int panDeltaY2 = Math.min(0, getBottom() - r.bottom);

        final int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
        final int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

        if (panDeltaX != 0 || panDeltaY != 0) {
            panBy(panDeltaX, panDeltaY);
        }
    }

    private void updateHighlightViewMatrix() {
        if (highlightView != null) {
            highlightView.matrix.set(previewImage.getUnrotatedPreviewMatrix(highlightView.matrix));
            highlightView.invalidate();
        }
    }
}
