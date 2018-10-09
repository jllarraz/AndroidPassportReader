/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example.jllarraz.com.passportreader.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import org.jmrtd.lds.icao.MRZInfo;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.asynctask.OcrRecognizeMlKitAsyncTask;
import example.jllarraz.com.passportreader.mlkit.OcrMrzDetectorProcessor;
import example.jllarraz.com.passportreader.mlkit.VisionProcessorBase;
import example.jllarraz.com.passportreader.ui.views.AutoFitTextureView;
import example.jllarraz.com.passportreader.utils.MRZUtil;
import example.jllarraz.com.passportreader.utils.camera.Camera2Manager;

public class Camera2MLKitFragment extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = Camera2MLKitFragment.class.getSimpleName();

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "Camera2MLKitFragment";

    private AutoFitTextureView mTextureView;

    /**
     * Camera 2 Api Manager
     */
    private Camera2Manager camera2Manager;

    private TextView mStatusBar;
    private TextView mStatusRead;


    ////////////////////////////////////////

    private Camera2MLKitFragmentListener camera2MLKitFragmentListener;
    private OcrMrzDetectorProcessor frameProcessor;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public static Camera2MLKitFragment newInstance() {
        return new Camera2MLKitFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_mrz, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mStatusBar= (TextView) view.findViewById(R.id.status_view_bottom);
        mStatusRead = (TextView) view.findViewById(R.id.status_view_top);
        camera2Manager = new Camera2Manager(getActivity(), mTextureView, new Camera2Manager.CameraManagerListener(){
            private  boolean isDecoding=false;
            @Override
            public void onError() {
                FragmentActivity activity = getActivity();
                if(activity!=null){
                    activity.onBackPressed();
                }
            }

            @Override
            public void onCamera2ApiNotSupported() {
                ErrorDialog.newInstance(getString(R.string.camera_error))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }

            @Override
            public void onConfigurationFailed() {
                showToast("Failed");
            }

            @Override
            public void onPermissionRequest() {
                requestCameraPermission();
            }

            @Override
            public void onPreviewCroppedImage(Bitmap bitmap) {
                if(!isDecoding) {
                    isDecoding = true;
                    OcrRecognizeMlKitAsyncTask ocrRecognizeMlKitAsyncTask = new OcrRecognizeMlKitAsyncTask(getContext().getApplicationContext(),
                            frameProcessor,
                            bitmap,
                            new VisionProcessorBase.OcrListener() {
                                @Override
                                public void onMRZRead(MRZInfo mrzInfo, long timeRequired) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                mStatusRead.setText(getString(R.string.status_bar_ocr, mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry()));
                                                mStatusBar.setText(getString(R.string.status_bar_success, timeRequired));
                                                mStatusBar.setTextColor(getResources().getColor(R.color.status_text));
                                                if(camera2MLKitFragmentListener!=null){
                                                    camera2MLKitFragmentListener.onPassportRead(mrzInfo);
                                                }

                                            }catch (IllegalStateException e){
                                                //The fragment is destroyed
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onMRZReadFailure(long timeRequired) {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                mStatusBar.setText(getString(R.string.status_bar_failure, timeRequired));
                                                mStatusBar.setTextColor(Color.RED);
                                                mStatusRead.setText("");
                                            }catch (IllegalStateException e){
                                                //The fragment is destroyed
                                            }
                                        }
                                    });

                                    isDecoding = false;
                                }

                                @Override
                                public void onFailure(Exception e, long timeRequired) {
                                    isDecoding = false;
                                    e.printStackTrace();
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(camera2MLKitFragmentListener!=null){
                                                camera2MLKitFragmentListener.onError();
                                            }
                                        }
                                    });
                                }
                    });
                    ocrRecognizeMlKitAsyncTask.execute();


                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        MRZUtil.cleanStorage();
        frameProcessor =  new OcrMrzDetectorProcessor();
        camera2Manager.startCamera();
    }

    @Override
    public void onPause() {
        camera2Manager.stopCamera();
        frameProcessor.stop();
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if(activity instanceof Camera2MLKitFragmentListener){
            camera2MLKitFragmentListener = (Camera2MLKitFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        camera2MLKitFragmentListener = null;
        super.onDetach();

    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Permissions
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.permission_camera_rationale))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }



    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Dialogs UI
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_camera_rationale)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Listener
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    public interface Camera2MLKitFragmentListener {
        void onPassportRead(MRZInfo mrzInfo);
        void onError();
    }


}
