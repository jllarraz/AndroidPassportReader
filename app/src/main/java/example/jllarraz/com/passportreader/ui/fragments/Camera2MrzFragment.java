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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;


import org.jmrtd.lds.MRZInfo;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.asynctask.OcrInitAsyncTask;
import example.jllarraz.com.passportreader.asynctask.OcrRecognizeAsyncTask;
import example.jllarraz.com.passportreader.common.PreferencesKeys;

import example.jllarraz.com.passportreader.data.OcrResult;
import example.jllarraz.com.passportreader.data.OcrResultFailure;
import example.jllarraz.com.passportreader.ui.views.AutoFitTextureView;
import example.jllarraz.com.passportreader.ui.views.ViewfinderView;
import example.jllarraz.com.passportreader.utils.MRZUtil;
import example.jllarraz.com.passportreader.utils.camera.Camera2Manager;

public class Camera2MrzFragment extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = Camera2MrzFragment.class.getSimpleName();

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "Camera2MrzFragment";

    private AutoFitTextureView mTextureView;

    /**
     * Camera 2 Api Manager
     */
    private Camera2Manager camera2Manager;

    /**
     * To allow user to select cropping area
     */
    private ViewfinderView viewfinderView;

    private TextView mStatusBar;
    private TextView mStatusRead;


    ////////////////////////////////////////
    /**
     * Languages for which Cube data is available.
     */
    static final String[] CUBE_SUPPORTED_LANGUAGES = {
            "ara", // Arabic
            "hin" // Hindi
    };
    private boolean isEngineReady;

    private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
    private int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
    private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
    private String sourceLanguageCodeOcr = "eng";
    private String mCharacterBlacklist="";
    private String mCharacterWhitelist="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<";

    private static String PASSPORT_LINE_1 ="[P]{1}[A-Z<]{1}[A-Z<]{3}[A-Z0-9<]{39}$";
    private static String PASSPORT_LINE_2 ="[A-Z0-9<]{9}[0-9]{1}[A-Z<]{3}[0-9]{6}[0-9]{1}[FM<]{1}[0-9]{6}[0-9]{1}[A-Z0-9<]{14}[0-9<]{1}[0-9]{1}$";

    private Camera2MrzFragmentListener camera2MrzFragmentListener;

    public static Camera2MrzFragment newInstance() {
        return new Camera2MrzFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_mrz, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        //view.findViewById(R.id.picture).setOnClickListener(this);
        //view.findViewById(R.id.info).setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        viewfinderView = (ViewfinderView) view.findViewById(R.id.viewfinder_view);
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
            public void onPreviewImage(Bitmap bitmap) {
                if(!isDecoding) {
                    isDecoding = true;
                    OcrRecognizeAsyncTask recognizeAsyncTask = new OcrRecognizeAsyncTask(getContext(), baseApi, bitmap, new OcrRecognizeAsyncTask.OcrRecognizeAsyncTaskListener() {
                        @Override
                        public void onError() {
                            Log.d(TAG, "OCR onError");
                            if (baseApi != null) {
                                baseApi.clear();
                            }
                            isDecoding = false;
                            if(camera2MrzFragmentListener !=null){
                                camera2MrzFragmentListener.onError();
                            }
                        }

                        @Override
                        public void onSuccess(OcrResult ocrResult) {
                            if (baseApi != null) {
                                baseApi.clear();
                            }
                            isDecoding = false;

                            onOCRSuccess(ocrResult);

                        }

                        @Override
                        public void onFailure(OcrResultFailure ocrResult) {
                            Log.e(TAG, "OCR FAILED: ");

                            if (baseApi != null) {
                                baseApi.clear();
                            }

                            onOCRFailure(ocrResult);

                            isDecoding = false;
                        }
                    });

                    recognizeAsyncTask.execute();
                }
            }
        });
        viewfinderView.setCameraManager(camera2Manager);
        // Set listener to change the size of the viewfinder rectangle.
        viewfinderView.setOnTouchListener(new View.OnTouchListener() {
            int lastX = -1;
            int lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = -1;
                        lastY = -1;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();

                        try {
                            Rect rect = camera2Manager.getFramingRect();

                            final int BUFFER = 50;
                            final int BIG_BUFFER = 60;
                            if (lastX >= 0) {
                                // Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
                                if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top left corner: adjust both top and left sides
                                    camera2Manager.adjustFramingRect(2 * (lastX - currentX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top right corner: adjust both top and right sides
                                    camera2Manager.adjustFramingRect(2 * (currentX - lastX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom left corner: adjust both bottom and left sides
                                    camera2Manager.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom right corner: adjust both bottom and right sides
                                    camera2Manager.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
                                    camera2Manager.adjustFramingRect(2 * (lastX - currentX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
                                    camera2Manager.adjustFramingRect(2 * (currentX - lastX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
                                    camera2Manager.adjustFramingRect(0, 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
                                    camera2Manager.adjustFramingRect(0, 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                }
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Framing rect not available", e);
                        }
                        v.invalidate();
                        lastX = currentX;
                        lastY = currentY;
                        return true;
                    case MotionEvent.ACTION_UP:
                        lastX = -1;
                        lastY = -1;
                        return true;
                }
                return false;
            }
        });


        isEngineReady = false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        MRZUtil.cleanStorage();
        camera2Manager.startCamera();

        // Do OCR engine initialization, if necessary
        int previousOcrEngineMode = ocrEngineMode;

        boolean doNewInit = (baseApi == null) || ocrEngineMode != previousOcrEngineMode;
        if (doNewInit) {
            // Initialize the OCR engine
            File storageDirectory = getStorageDirectory();
            if (storageDirectory != null) {
                initOcrEngine(storageDirectory, sourceLanguageCodeOcr, "eng");
            }
        } else {
            // We already have the engine initialized, so just start the camera.
            resumeOCR();
        }
    }

    @Override
    public void onPause() {
        camera2Manager.stopCamera();

        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if(activity instanceof Camera2MrzFragmentListener){
            camera2MrzFragmentListener = (Camera2MrzFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        camera2MrzFragmentListener = null;
        super.onDetach();

    }

    protected void onOCRSuccess(OcrResult ocrResult){

        String result = ocrResult.getText();
        if (result != null && !"".equals(result)) {
            //Clean OCR String
            result.replaceAll(" ", "");
            String[] textResultTmpArr = result.split("\n");
            result = "";
            for (int i = 0; i < textResultTmpArr.length; i++) {
                if (textResultTmpArr[i].length() > 10) {
                    result += textResultTmpArr[i] + '\n';
                }
            }
            result = result.replaceAll(" ", "");
            ocrResult.setText(result);
            //Match OCR with Passport REGEX
            try {
                //Modify on the flight bad read, when we are sure that it's safe
                result = MRZUtil.cleanString(result);
                ocrResult.setText(result);
            }catch (IllegalArgumentException e){
                //Invalid read
                //Keep going just to print
                e.printStackTrace();
            }

            Pattern patternLine1 = Pattern.compile(PASSPORT_LINE_1);
            Matcher matcherLine1 = patternLine1.matcher(result);
            Pattern patternLine2 = Pattern.compile(PASSPORT_LINE_2);
            Matcher matcherLine2 = patternLine2.matcher(result);

            Log.d(TAG, "Read: "+result);

            if(matcherLine1.find()){
                Log.d(TAG, "Line 1: "+matcherLine1.group(0));
                MRZUtil.addLine1(matcherLine1.group(0));
            }

            if(matcherLine2.find()){
                Log.d(TAG, "Line 2: "+matcherLine2.group(0));
                MRZUtil.addLine2(matcherLine2.group(0));
            }

            try {
                MRZInfo mrzInfo = MRZUtil.getMRZInfo();
                //Toast.makeText(getContext(), mrzInfo.toString(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "MRZ READ: " + mrzInfo.toString());
                if(camera2MrzFragmentListener !=null){
                    camera2MrzFragmentListener.onPassportRead(mrzInfo);
                }
                MRZUtil.cleanStorage();
            }catch (Exception e){

            }

            //If lines match passport format
            /*if(matcherLine1.find()&&matcherLine2.find()) {
                if (ocrResult.getMeanConfidence() >= 60 && textResultTmpArr.length >= 2 && textResultTmpArr.length <= 3) {
                    try {
                        //Check validation checksums
                        MRZInfo mrzInfo = new MRZInfo(result);
                        if (mrzInfo.toString().equals(result)) {
                            Toast.makeText(getContext(), mrzInfo.toString(), Toast.LENGTH_LONG).show();
                            Log.d(TAG, "MRZ READ: " + mrzInfo.toString());
                        }
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        Log.w("CACA", "checksum fail", e);
                    }
                }
            }*/
        }
        try {
            mStatusRead.setText(ocrResult.getText());
            mStatusBar.setText(getString(R.string.status_bar_success, ocrResult.getMeanConfidence(), ocrResult.getRecognitionTimeRequired()));
            mStatusBar.setTextColor(getResources().getColor(R.color.status_text));
        }catch (IllegalStateException e){
            //The fragment is destroyed
        }

        Log.d(TAG, ocrResult.getText());
    }

    protected void onOCRFailure(OcrResultFailure ocrResult){
        try {
            mStatusBar.setText(getString(R.string.status_bar_failure, ocrResult.getTimeRequired()));
            mStatusBar.setTextColor(Color.RED);
            mStatusRead.setText("");
        }catch (IllegalStateException e){
        //The fragment is destroyed
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //       Configure Tesseract
    //
    ////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Requests initialization of the OCR engine with the given parameters.
     *
     * @param storageRoot  Path to location of the tessdata directory to use
     * @param languageCode Three-letter ISO 639-3 language code for OCR
     * @param languageName Name of the language for OCR, for example, "English"
     */
    private void initOcrEngine(File storageRoot, String languageCode, String languageName) {
        isEngineReady = false;
        // If our language doesn't support Cube, then set the ocrEngineMode to Tesseract
        if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {
            boolean cubeOk = false;
            for (String s : CUBE_SUPPORTED_LANGUAGES) {
                if (s.equals(languageCode)) {
                    cubeOk = true;
                }
            }
            if (!cubeOk) {
                ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                prefs.edit().putString(PreferencesKeys.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
            }
        }

        // Start AsyncTask to install language data and init OCR
        baseApi = new TessBaseAPI();
        new OcrInitAsyncTask(getContext().getApplicationContext(), baseApi, languageCode, languageName, ocrEngineMode, new OcrInitAsyncTask.OcrInitAsyncTaskListener(){

            @Override
            public void onError() {
                Toast.makeText(getContext(), getString(R.string.warning_error_ocr_install), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess() {
                resumeOCR();
            }

            @Override
            public void onProgress(int percentage) {
                Log.d(TAG, getString(R.string.warning_ocr_install_progress, percentage));
            }

            @Override
            public void onStart() {
                Toast.makeText(getContext(), getString(R.string.warning_ocr_install), Toast.LENGTH_SHORT).show();
            }
        })
        .execute(storageRoot.toString());
    }

    /**
     * Method to start or restart recognition after the OCR engine has been initialized,
     * or after the app regains focus. Sets state related settings and OCR engine parameters,
     * and requests camera initialization.
     */
    void resumeOCR() {
        Log.d(TAG, "resumeOCR()");
        // This method is called when Tesseract has already been successfully initialized, so set
        // isEngineReady = true here.
        isEngineReady = true;

        if (baseApi != null) {
            baseApi.setPageSegMode(pageSegmentationMode);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, mCharacterBlacklist);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, mCharacterWhitelist);
        }
    }

    /**
     * Returns a string that represents which OCR engine(s) are currently set to be run.
     *
     * @return OCR engine mode
     */
    String getOcrEngineModeName() {
        String ocrEngineModeName = "";
        String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
        if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_ONLY) {
            ocrEngineModeName = ocrEngineModes[0];
        } else if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY) {
            ocrEngineModeName = ocrEngineModes[1];
        } else if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
            ocrEngineModeName = ocrEngineModes[2];
        }
        return ocrEngineModeName;
    }

    /**
     * Finds the proper location on the cache dir where we can save files.
     */
    private File getStorageDirectory() {
        return getContext().getCacheDir();
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

    public interface Camera2MrzFragmentListener {
        void onPassportRead(MRZInfo mrzInfo);
        void onError();
    }


}
