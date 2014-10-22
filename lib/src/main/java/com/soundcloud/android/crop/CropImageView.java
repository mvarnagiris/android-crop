package com.soundcloud.android.crop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CropImageView extends BaseCropImageView {
    private HighlightView highlightView;

    private float lastX;
    private float lastY;
    private int motionEdge;

    public CropImageView(Context context) {
        super(context);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (previewBitmap.getBitmap() != null && highlightView != null) {
            highlightView.matrix.set(getUnrotatedPreviewMatrix());
            highlightView.invalidate();
            if (highlightView.hasFocus()) {
                centerBasedOnHighlightView(highlightView);
            }
        }
    }

    @Override
    protected void zoomTo(float scale, float centerX, float centerY) {
        super.zoomTo(scale, centerX, centerY);
        if (highlightView != null) {
            highlightView.matrix.set(getUnrotatedPreviewMatrix());
            highlightView.invalidate();
        }
    }

    @Override
    protected void zoomIn() {
        super.zoomIn();
        if (highlightView != null) {
            highlightView.matrix.set(getUnrotatedPreviewMatrix());
            highlightView.invalidate();
        }
    }

    @Override
    protected void zoomOut() {
        super.zoomOut();
        if (highlightView != null) {
            highlightView.matrix.set(getUnrotatedPreviewMatrix());
            highlightView.invalidate();
        }
    }

    @Override
    protected void postTranslate(float deltaX, float deltaY) {
        super.postTranslate(deltaX, deltaY);
        if (highlightView != null) {
            highlightView.matrix.postTranslate(deltaX, deltaY);
            highlightView.invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

                // Ff we're not zoomed then there's no point in even allowing the user to move the
                // image around. This call to center puts it back to the normalized location.
                if (getScale() == 1F) {
                    center(true, true);
                }
                break;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (highlightView != null) {
            highlightView.draw(canvas);
        }
    }

    public HighlightView getHighlightView() {
        return highlightView;
    }

    public void setHighlightView(HighlightView highlightView) {
        this.highlightView = highlightView;
        invalidate();
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
        zoom = zoom * this.getScale();
        zoom = Math.max(1f, zoom);

        if ((Math.abs(zoom - getScale()) / zoom) > .1) {
            float[] coordinates = new float[]{hv.cropRect.centerX(), hv.cropRect.centerY()};
            getUnrotatedPreviewMatrix().mapPoints(coordinates);
            zoomTo(zoom, coordinates[0], coordinates[1], 300F);
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
}
