package com.soundcloud.android.crop;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;

class PreviewImage {
    private final PreviewSize previewSize;
    private final RotateBitmap rotateBitmap;
    private final Matrix baseMatrix = new Matrix();
    private final Matrix transformationMatrix = new Matrix();
    private final Matrix previewMatrix = new Matrix();
    private final float[] matrixBuffer = new float[9];
    private final int sampleSize;

    private PreviewImageListener listener;

    public PreviewImage(ContentResolver contentResolver, PreviewSize previewSize, SourceImage sourceImage) throws Exception {
        this.previewSize = previewSize;

        sampleSize = calculateSampleSize(previewSize, sourceImage);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        final Bitmap tempBitmap = sourceImage.decodeBitmap(contentResolver, options);
        final int exifRotation = sourceImage.getExifRotation();

        rotateBitmap = new RotateBitmap(tempBitmap, exifRotation);
        calculateBaseMatrix(true, baseMatrix);
    }

    public RotateBitmap getRotateBitmap() {
        return rotateBitmap;
    }

    public Bitmap getBitmap() {
        return rotateBitmap.getBitmap();
    }

    public int getWidth() {
        return rotateBitmap.getWidth();
    }

    public int getHeight() {
        return rotateBitmap.getHeight();
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setListener(PreviewImageListener listener) {
        this.listener = listener;
    }

    public void center(boolean horizontal, boolean vertical) {
        final Bitmap bitmap = rotateBitmap.getBitmap();
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
            int viewHeight = previewSize.getHeight();
            if (height < viewHeight) {
                deltaY = (viewHeight - height) / 2 - rect.top;
            } else if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = viewHeight - rect.bottom;
            }
        }

        if (horizontal) {
            int viewWidth = previewSize.getWidth();
            if (width < viewWidth) {
                deltaX = (viewWidth - width) / 2 - rect.left;
            } else if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
            }
        }

        postTranslate(deltaX, deltaY);
        notifyListener();
    }

    public void zoomTo(float scale, float centerX, float centerY) {
        final float maxZoom = getMaxZoom();
        if (scale > maxZoom) {
            scale = maxZoom;
        }

        float oldScale = getScale();
        float deltaScale = scale / oldScale;

        transformationMatrix.postScale(deltaScale, deltaScale, centerX, centerY);
        center(true, true);
        notifyListener();
    }

    public void zoomIn(float rate) {
        if (rotateBitmap.getBitmap() == null) {
            return;
        }

        if (getScale() >= getMaxZoom()) {
            return; // Don't let the user zoom into the molecular level
        }

        float cx = previewSize.getWidth() / 2f;
        float cy = previewSize.getHeight() / 2f;

        transformationMatrix.postScale(rate, rate, cx, cy);
        notifyListener();
    }

    public void zoomOut(float rate) {
        if (rotateBitmap.getBitmap() == null) {
            return;
        }

        float cx = previewSize.getWidth() / 2f;
        float cy = previewSize.getHeight() / 2f;

        // Zoom out to at most 1x
        Matrix tmp = new Matrix(transformationMatrix);
        tmp.postScale(1f / rate, 1f / rate, cx, cy);

        if (getScale(tmp) < 1f) {
            transformationMatrix.setScale(1f, 1f, cx, cy);
        } else {
            transformationMatrix.postScale(1f / rate, 1f / rate, cx, cy);
        }

        center(true, true);
        notifyListener();
    }

    public void panBy(float dx, float dy) {
        postTranslate(dx, dy);
        notifyListener();
    }

    public Matrix getPreviewMatrix() {
        previewMatrix.set(baseMatrix);
        previewMatrix.postConcat(transformationMatrix);
        return previewMatrix;
    }

    public Matrix getUnrotatedPreviewMatrix(Matrix matrix) {
        if (matrix == null) {
            matrix = new Matrix();
        }
        calculateBaseMatrix(false, matrix);
        matrix.postConcat(transformationMatrix);
        return matrix;
    }

    public float getScale() {
        return getScale(transformationMatrix);
    }

    private int calculateSampleSize(PreviewSize previewSize, SourceImage sourceImage) throws Exception {
        final int height = sourceImage.getHeight();
        final int width = sourceImage.getWidth();
        int viewHeight = previewSize.getHeight();
        int viewWidth = previewSize.getWidth();
        int inSampleSize = 1;

        if (height > viewHeight || width > viewWidth) {
            // Do a bit more aggressive sampling. Assume that the max size we need is scaled down image
            // that fits within a view.
            final int dX = width - viewWidth;
            final int dY = height - viewHeight;
            if (dX > dY) {
                viewHeight = (int) ((float) height * viewWidth / width);
            } else {
                viewWidth = (int) ((float) width * viewHeight / height);
            }

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > viewHeight && (halfWidth / inSampleSize) > viewWidth) {
                inSampleSize++;
            }
        }

        return inSampleSize;
    }

    private Matrix calculateBaseMatrix(boolean includeRotation, Matrix matrix) {
        if (matrix == null) {
            matrix = new Matrix();
        }
        float viewWidth = previewSize.getWidth();
        float viewHeight = previewSize.getHeight();

        float w = rotateBitmap.getWidth();
        float h = rotateBitmap.getHeight();
        matrix.reset();

        // We limit up-scaling to 3x otherwise the result may look bad if it's a small icon
        float widthScale = Math.min(viewWidth / w, 3.0f);
        float heightScale = Math.min(viewHeight / h, 3.0f);
        float scale = Math.min(widthScale, heightScale);

        if (includeRotation) {
            matrix.postConcat(rotateBitmap.getRotateMatrix());
        }
        matrix.postScale(scale, scale);
        matrix.postTranslate((viewWidth - w * scale) / 2F, (viewHeight - h * scale) / 2F);
        return matrix;
    }

    private float getScale(Matrix matrix) {
        return getValueFromMatrix(matrix, Matrix.MSCALE_X);
    }

    private float getValueFromMatrix(Matrix matrix, int whichValue) {
        matrix.getValues(matrixBuffer);
        return matrixBuffer[whichValue];
    }

    private float getMaxZoom() {
        if (rotateBitmap.getBitmap() == null) {
            return 1f;
        }

        float fw = (float) rotateBitmap.getWidth() / previewSize.getWidth();
        float fh = (float) rotateBitmap.getHeight() / previewSize.getHeight();
        return Math.max(fw, fh) * 4; // 400%
    }

    private void postTranslate(float dx, float dy) {
        transformationMatrix.postTranslate(dx, dy);
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onPreviewImageChanged(getPreviewMatrix());
        }
    }

    public static interface PreviewImageListener {
        public void onPreviewImageChanged(Matrix matrix);
    }
}
