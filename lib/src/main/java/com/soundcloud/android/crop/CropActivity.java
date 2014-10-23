package com.soundcloud.android.crop;

import android.os.Bundle;
import android.widget.Toast;

public class CropActivity extends BaseCropActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop__activity_crop);
    }

    @Override
    protected CropImageView getCropImageView() {
        return (CropImageView) findViewById(R.id.crop_image);
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
}
