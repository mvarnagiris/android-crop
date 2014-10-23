package com.soundcloud.android.crop;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.FileNotFoundException;
import java.io.OutputStream;

public class SaveFragment extends Fragment {
    private static final String ARG_SOURCE_IMAGE = "ARG_SOURCE_IMAGE";
    private static final String ARG_CROP_CONFIG = "ARG_CROP_CONFIG";
    private static final String ARG_CROP_RECT = "ARG_CROP_RECT";

    private SaveListener listener;
    private SourceImage sourceImage;
    private CropConfig cropConfig;
    private Rect cropRect;
    private ContentResolver contentResolver;

    public static SaveFragment newInstance(SourceImage sourceImage, CropConfig cropConfig, Rect cropRect) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_SOURCE_IMAGE, sourceImage);
        args.putParcelable(ARG_CROP_CONFIG, cropConfig);
        args.putParcelable(ARG_CROP_RECT, cropRect);

        final SaveFragment fragment = new SaveFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SaveListener) {
            listener = (SaveListener) activity;
        } else {
            throw new IllegalArgumentException("Activity " + activity.getClass().getName() + " must implement " + SaveListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        sourceImage = getArguments().getParcelable(ARG_SOURCE_IMAGE);
        cropConfig = getArguments().getParcelable(ARG_CROP_CONFIG);
        cropRect = getArguments().getParcelable(ARG_CROP_RECT);
        contentResolver = getActivity().getContentResolver();

        new SaveTask().execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public static interface SaveListener {
        public void onSaveStarted();

        public void onSaveFinished();

        public void onSaveFailed(Exception error);
    }

    private class SaveTask extends AsyncTask<Void, Void, Exception> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (listener != null) {
                listener.onSaveStarted();
            }
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                save();
            } catch (Exception error) {
                return error;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Exception error) {
            super.onPostExecute(error);
            if (listener != null) {
                if (error == null) {
                    listener.onSaveFinished();
                } else {
                    listener.onSaveFailed(error);
                }
            }
        }

        private void save() throws Exception {
            final Point cropSize = getCropSize();
            final int outWidth = cropSize.x;
            final int outHeight = cropSize.y;

            Bitmap croppedImage = sourceImage.decodeRegion(contentResolver, cropRect, outWidth, outHeight);
            croppedImage = resizeImage(croppedImage, outWidth, outHeight);
            saveImage(croppedImage);
        }

        private Point getCropSize() {
            final int width = cropRect.width();
            final int height = cropRect.height();
            final int maxX = cropConfig.getMaxX();
            final int maxY = cropConfig.getMaxY();

            int outWidth = width;
            int outHeight = height;
            if (maxX > 0 && maxY > 0 && (width > maxX || height > maxY)) {
                float ratio = (float) width / (float) height;
                if ((float) maxX / (float) maxY > ratio) {
                    outHeight = maxY;
                    outWidth = (int) ((float) maxY * ratio + 0.5f);
                } else {
                    outWidth = maxX;
                    outHeight = (int) ((float) maxX / ratio + 0.5f);
                }
            }
            return new Point(outWidth, outHeight);
        }

        private Bitmap resizeImage(Bitmap croppedImage, int outWidth, int outHeight) throws Exception {
            if (croppedImage == null || (croppedImage.getWidth() <= outWidth && croppedImage.getHeight() <= outHeight)) {
                return croppedImage;
            }

            final Bitmap newBitmap;
            try {
                newBitmap = Bitmap.createScaledBitmap(croppedImage, outWidth, outHeight, true);
            } catch (OutOfMemoryError error) {
                throw new Exception("Out of memory.");
            }

            if (newBitmap != croppedImage) {
                croppedImage.recycle();
            }
            return newBitmap;
        }

        private void saveImage(Bitmap croppedImage) throws FileNotFoundException {
            OutputStream outputStream = null;
            try {
                outputStream = contentResolver.openOutputStream(cropConfig.getOutputUri());
                croppedImage.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            } finally {
                CropUtil.closeSilently(outputStream);
            }


            CropUtil.copyExifRotation(
                    CropUtil.getFromMediaUri(contentResolver, sourceImage.getUri()),
                    CropUtil.getFromMediaUri(contentResolver, cropConfig.getOutputUri())
            );

            croppedImage.recycle();
        }
    }
}
