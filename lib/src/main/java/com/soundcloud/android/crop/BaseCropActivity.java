package com.soundcloud.android.crop;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver;

public abstract class BaseCropActivity extends Activity implements SetupFragment.SetupListener, SaveFragment.SaveListener {
    public static final int RESULT_ERROR = 827473;

    public static final String RESULT_EXTRA_ERROR = BaseCropActivity.class.getName() + ".RESULT_EXTRA_ERROR";

    private static final String FRAGMENT_SETUP = BaseCropActivity.class.getName() + "FRAGMENT_SETUP";
    private static final String FRAGMENT_SAVE = BaseCropActivity.class.getName() + "FRAGMENT_SAVE";

    protected CropImageView cropImageView;

    protected PreviewSize previewSize;
    protected SourceImage sourceImage;
    protected PreviewImage previewImage;
    protected CropConfig cropConfig;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        cropImageView = getCropImageView();
        if (cropImageView == null) {
            throw new NullPointerException("Crop image view cannot be null.");
        }

        cropConfig = CropConfig.from(getIntent());
        startProcessing();
        waitForLayout();
    }

    @Override
    public void onSetupStarted() {
        if (cropImageView.isEnabled()) {
            startProcessing();
        }
    }

    @Override
    public void onSetupFinished(SourceImage sourceImage, PreviewImage previewImage) {
        removeFragment(FRAGMENT_SETUP);
        this.sourceImage = sourceImage;
        this.previewImage = previewImage;
        startCrop();
        stopProcessing(false);
    }

    @Override
    public void onSetupFailed(Exception error) {
        failure(error, FRAGMENT_SETUP);
    }

    @Override
    public void onSaveStarted() {
        if (cropImageView.isEnabled()) {
            startProcessing();
        }
    }

    @Override
    public void onSaveFinished() {
        removeFragment(FRAGMENT_SAVE);
        stopProcessing(true);

        final Intent data = new Intent();
        data.setData(cropConfig.getOutputUri());
        setResult(RESULT_OK, data);
    }

    @Override
    public void onSaveFailed(Exception error) {
        failure(error, FRAGMENT_SAVE);
    }

    protected void onInitialLayoutFinished() {
        previewSize = new PreviewSize(cropImageView);
        startSetupIfNotStarted();
    }

    protected void save() {
        if (isSaving()) {
            return;
        }

        final Rect cropRect = cropImageView.getHighlightView().getScaledCropRect(previewImage.getSampleSize());
        cropImageView.setPreviewImage(null);
        final SaveFragment fragment = SaveFragment.newInstance(sourceImage, previewImage.getRotateBitmap().getRotateMatrix(), previewImage.getRotateBitmap().getRotation(), cropConfig, cropRect);
        getFragmentManager().beginTransaction().add(fragment, FRAGMENT_SETUP).commit();
    }

    protected abstract CropImageView getCropImageView();

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
        } else {
            onFinishProcessing();
        }
    }

    private void startCrop() {
        cropImageView.setPreviewImage(previewImage);
        cropImageView.center(true, true);

        cropImageView.setHighlightView(getHighlightView());
        cropImageView.getHighlightView().setFocus(true);
    }

    private HighlightView getHighlightView() {
        final HighlightView highlightView = new HighlightView(cropImageView);
        final int width = previewImage.getWidth();
        final int height = previewImage.getHeight();

        final Rect imageRect = new Rect(0, 0, width, height);
        int cropWidth = Math.min(width, height) * 99 / 100;
        //noinspection SuspiciousNameCombination
        int cropHeight = cropWidth;

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
        highlightView.setup(previewImage.getUnrotatedPreviewMatrix(null), imageRect, cropRect, aspectX != 0 && aspectY != 0);

        return highlightView;
    }

    private void removeFragment(String tag) {
        final Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    private boolean isSaving() {
        return getFragmentManager().findFragmentByTag(FRAGMENT_SAVE) != null;
    }

    private void startProcessing() {
        cropImageView.setEnabled(false);
        onStartProcessing();
    }

    private void stopProcessing(boolean isDone) {
        if (!isDone) {
            cropImageView.setEnabled(true);
        }
        onFinishProcessing();
    }

    private void failure(Exception error, String fragmentTag) {
        removeFragment(fragmentTag);
        stopProcessing(true);

        final Intent data = new Intent();
        data.putExtra(RESULT_EXTRA_ERROR, error);
        setResult(RESULT_ERROR, data);

        onShowError(error);
    }
}
