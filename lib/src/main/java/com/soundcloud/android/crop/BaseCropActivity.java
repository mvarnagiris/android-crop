package com.soundcloud.android.crop;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver;

public abstract class BaseCropActivity extends Activity implements SetupFragment.SetupListener {
    public static final int RESULT_ERROR = 827473;

    public static final String RESULT_EXTRA_ERROR = BaseCropActivity.class.getName() + ".RESULT_EXTRA_ERROR";

    private static final String FRAGMENT_SETUP = BaseCropActivity.class.getName() + "FRAGMENT_SETUP";

    protected BaseCropImageView cropImageView;

    protected PreviewSize previewSize;
    protected SourceImage sourceImage;
    protected PreviewImage previewImage;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        cropImageView = getCropImageView();
        if (cropImageView == null) {
            throw new NullPointerException("Crop image view cannot be null.");
        }

        onStartProcessing();
        waitForLayout();
    }

    @Override
    public void onSetupStarted() {
        onStartProcessing();
    }

    @Override
    public void onSetupFinished(SourceImage sourceImage, PreviewImage previewImage) {
        this.sourceImage = sourceImage;
        this.previewImage = previewImage;
        onFinishProcessing();
        startCrop();
    }

    @Override
    public void onSetupFailed(Exception error) {
        onFinishProcessing();
        onShowError(error);
    }

    protected void onInitialLayoutFinished() {
        previewSize = new PreviewSize(cropImageView);
        startSetupIfNotStarted();
    }

    protected abstract BaseCropImageView getCropImageView();

    protected abstract void onStartProcessing();

    protected abstract void onFinishProcessing();

    protected abstract void onShowError(Exception error);

    private void waitForLayout() {
        findViewById(android.R.id.content).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    findViewById(android.R.id.content).getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    //noinspection deprecation
                    findViewById(android.R.id.content).getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                onInitialLayoutFinished();
            }
        });
    }

    private void startSetupIfNotStarted() {
        SetupFragment fragment = (SetupFragment) getFragmentManager().findFragmentByTag(FRAGMENT_SETUP);
        if (fragment == null) {
            fragment = SetupFragment.newInstance(previewSize);
            getFragmentManager().beginTransaction().add(fragment, FRAGMENT_SETUP).commit();
        }
    }

    private void startCrop() {
        cropImageView.setImageBitmap(previewImage.getBitmap());
        cropImageView.center(true, true);

        // TODO This is wrong. Highlight view should probably be withing BaseCropImageView.
        ((CropImageView) cropImageView).setHighlightView(getHighlightView());
        ((CropImageView) cropImageView).getHighlightView().setFocus(true);
    }

    private HighlightView getHighlightView() {
        final HighlightView highlightView = new HighlightView(cropImageView);
        final int width = previewImage.getWidth();
        final int height = previewImage.getHeight();

        final Rect imageRect = new Rect(0, 0, width, height);
        int cropWidth = Math.min(width, height) * 99 / 100;
        //noinspection SuspiciousNameCombination
        int cropHeight = cropWidth;

        final CropConfig cropConfig = CropConfig.from(getIntent());
        final int aspectX = cropConfig.getAspectX();
        final int aspectY = cropConfig.getAspectY();
        if (aspectX != 0 && aspectY != 0) {
            if (aspectX > aspectY) {
                cropHeight = cropWidth * aspectY / aspectX;
            } else {
                cropWidth = cropHeight * aspectX / aspectY;
            }
        }

        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;

        final RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
        highlightView.setup(cropImageView.getUnrotatedPreviewMatrix(), imageRect, cropRect, aspectX != 0 && aspectY != 0);

        return highlightView;
    }
}
