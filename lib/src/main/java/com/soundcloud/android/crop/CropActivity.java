package com.soundcloud.android.crop;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

public class CropActivity extends BaseCropActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop__activity_crop);
    }

    @Override
    protected ImageViewTouchBase getCropImageView() {
        final ImageViewTouchBase view = (ImageViewTouchBase) findViewById(R.id.crop_image);
        view.setRecycler(new BitmapRecycler());
        return view;
    }

    @Override
    protected void onStartProcessing() {

    }

    @Override
    protected void onFinishProcessing() {

    }

    @Override
    protected void onShowError(Exception error) {
        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        finish();
    }

    private static class BitmapRecycler implements ImageViewTouchBase.Recycler {
        @Override
        public void recycle(Bitmap b) {
            b.recycle();
            System.gc();
        }
    }
}
