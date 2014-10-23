package com.soundcloud.android.crop;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class CropImageActivity extends BaseCropActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop__activity_crop);

        final View cancelView = findViewById(R.id.btn_cancel);
        final View doneView = findViewById(R.id.btn_done);

        cancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        doneView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
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

    @Override
    public void onSaveFinished() {
        super.onSaveFinished();
        finish();
    }
}
