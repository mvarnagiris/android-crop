package com.soundcloud.android.crop;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

public class SetupFragment extends Fragment {
    private static final String ARG_PREVIEW_SIZE = "ARG_PREVIEW_SIZE";

    private SetupListener listener;
    private ContentResolver contentResolver;
    private Intent activityIntent;
    private PreviewSize previewSize;

    public static SetupFragment newInstance(PreviewSize previewSize) {
        final Bundle args = new Bundle();
        args.putSerializable(ARG_PREVIEW_SIZE, previewSize);

        final SetupFragment fragment = new SetupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SetupListener) {
            listener = (SetupListener) activity;
        } else {
            throw new IllegalArgumentException("Activity " + activity.getClass().getName() + " must implement " + SetupListener.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        contentResolver = getActivity().getContentResolver();
        activityIntent = getActivity().getIntent();
        previewSize = (PreviewSize) getArguments().getSerializable(ARG_PREVIEW_SIZE);

        new SetupTask().execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public static interface SetupListener {
        public void onSetupStarted();

        public void onSetupFinished(SourceImage sourceImage, PreviewImage previewImage);

        public void onSetupFailed(Exception error);
    }

    private class SetupTask extends AsyncTask<Void, Void, Exception> {
        private SourceImage sourceImage;
        private PreviewImage previewImage;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (listener != null) {
                listener.onSetupStarted();
            }
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                setup();
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
                    listener.onSetupFinished(sourceImage, previewImage);
                } else {
                    listener.onSetupFailed(error);
                }
            }
        }

        private void setup() throws Exception {
            sourceImage = new SourceImage(contentResolver, activityIntent);
            previewImage = new PreviewImage(contentResolver, previewSize, sourceImage);
        }
    }
}
