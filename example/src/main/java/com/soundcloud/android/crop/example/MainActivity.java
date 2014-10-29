package com.soundcloud.android.crop.example;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.soundcloud.android.crop.BaseCropActivity;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final int REQUEST_PICK = 1;
    private static final int REQUEST_CROP = 2;

    private ImageView resultView;

    private static File createImageFile(Activity activity) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + ".jpg";
        File path = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(path, imageFileName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultView = (ImageView) findViewById(R.id.result_image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select) {
            resultView.setImageDrawable(null);
            pickImage(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
    }

    private void beginCrop(Uri sourceUri) {
        Uri outputUri = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        final Intent intent = Crop.createIntentBuilder(this, sourceUri, outputUri).asSquare().withMaxSize(width, width).build();
        startActivityForResult(intent, REQUEST_CROP);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            resultView.setImageURI(Crop.getOutput(result));
        } else if (resultCode == BaseCropActivity.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Utility method that starts an image picker since that often precedes a crop
     *
     * @param activity Activity that will receive result
     */
    public Uri pickImage(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK).setType("image/*");
        Intent chooserIntent = Intent.createChooser(intent, null);

        File file = createImageFile(activity);
        Uri uri = Uri.fromFile(file);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri);
        Parcelable[] additionalIntents = {takePictureIntent};
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, additionalIntents);
        try {
            activity.startActivityForResult(chooserIntent, REQUEST_PICK);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.crop__pick_error, Toast.LENGTH_SHORT).show();
        }
        return uri;
    }
}
