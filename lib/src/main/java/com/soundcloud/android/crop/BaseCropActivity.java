package com.soundcloud.android.crop;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver;

public abstract class BaseCropActivity extends Activity implements SetupFragment.SetupListener {
    private static final String FRAGMENT_SETUP = BaseCropActivity.class.getName() + "FRAGMENT_SETUP";

    protected ImageViewTouchBase cropImageView;

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
        cropImageView.setBitmap(previewImage.getBitmap(), true);
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

    protected abstract ImageViewTouchBase getCropImageView();

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
}
