package com.soundcloud.android.crop;

import android.opengl.GLES10;
import android.view.View;

import java.io.Serializable;

class PreviewSize implements Serializable {
    final int width;
    final int height;

    PreviewSize(View previewView) {
        if (previewView == null) {
            throw new NullPointerException("Preview view cannot be null.");
        }

        final int viewWidth = previewView.getMeasuredWidth();
        final int viewHeight = previewView.getMeasuredHeight();

        if (viewWidth == 0 || viewHeight == 0) {
            throw new IllegalStateException("Preview view doesn't have a size. Make sure to call this only after layout finishes.");
        }

        final int maxTextureSize = getMaxTextureSize();
        if (maxTextureSize > 0) {
            width = Math.min(viewWidth, maxTextureSize);
            height = Math.min(viewHeight, maxTextureSize);
        } else {
            width = viewWidth;
            height = viewHeight;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private int getMaxTextureSize() {
        // The OpenGL texture size is the maximum size that can be drawn in an ImageView
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        return maxSize[0];
    }
}
