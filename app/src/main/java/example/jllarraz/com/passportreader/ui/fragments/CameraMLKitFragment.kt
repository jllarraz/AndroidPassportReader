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

package example.jllarraz.com.passportreader.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast


import org.jmrtd.lds.icao.MRZInfo

import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.asynctask.OcrRecognizeMlKitAsyncTask2
import example.jllarraz.com.passportreader.mlkit.OcrMrzDetectorProcessor
import example.jllarraz.com.passportreader.mlkit.VisionProcessorBase
import example.jllarraz.com.passportreader.utils.MRZUtil
import io.fotoapparat.Fotoapparat
import io.fotoapparat.preview.Frame
import io.fotoapparat.util.FrameProcessor
import kotlinx.android.synthetic.main.fragment_camera_mrz.*

class CameraMLKitFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {


    /**
     * Camera 2 Api Manager
     */
   // private var camera2Manager: Camera2Manager? = null

    private var fotoapparat: Fotoapparat?=null

    private var mStatusBar: TextView? = null
    private var mStatusRead: TextView? = null


    ////////////////////////////////////////

    private var camera2MLKitFragmentListener: Camera2MLKitFragmentListener? = null
    private var frameProcessor: OcrMrzDetectorProcessor? = null
    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_mrz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mStatusBar = view.findViewById<View>(R.id.status_view_bottom) as TextView
        mStatusRead = view.findViewById<View>(R.id.status_view_top) as TextView


        frameProcessor = OcrMrzDetectorProcessor()

        /*camera2Manager = Camera2Manager(context!!, mTextureView, object : Camera2Manager.CameraManagerListener {
            private var isDecoding = false
            override fun onError() {
                val activity = activity
                activity?.onBackPressed()
            }

            override fun onCamera2ApiNotSupported() {
                ErrorDialog.newInstance(getString(R.string.camera_error))
                        .show(childFragmentManager, FRAGMENT_DIALOG)
            }

            override fun onConfigurationFailed() {
                showToast("Failed")
            }

            override fun onPermissionRequest() {
                requestCameraPermission()
            }

            override fun onPreviewCroppedImage(bitmap: Bitmap?) {
                if (!isDecoding) {
                    isDecoding = true
                    val ocrRecognizeMlKitAsyncTask = OcrRecognizeMlKitAsyncTask(context!!.applicationContext,
                            frameProcessor!!,
                            bitmap!!,
                            object : VisionProcessorBase.OcrListener {
                                override fun onMRZRead(mrzInfo: MRZInfo, timeRequired: Long) {
                                    mHandler.post {
                                        try {
                                            mStatusRead!!.text = getString(R.string.status_bar_ocr, mrzInfo.documentNumber, mrzInfo.dateOfBirth, mrzInfo.dateOfExpiry)
                                            mStatusBar!!.text = getString(R.string.status_bar_success, timeRequired)
                                            mStatusBar!!.setTextColor(resources.getColor(R.color.status_text))
                                            if (camera2MLKitFragmentListener != null) {
                                                camera2MLKitFragmentListener!!.onPassportRead(mrzInfo)
                                            }

                                        } catch (e: IllegalStateException) {
                                            //The fragment is destroyed
                                        }
                                    }
                                }

                                override fun onMRZReadFailure(timeRequired: Long) {
                                    mHandler.post {
                                        try {
                                            mStatusBar!!.text = getString(R.string.status_bar_failure, timeRequired)
                                            mStatusBar!!.setTextColor(Color.RED)
                                            mStatusRead!!.text = ""
                                        } catch (e: IllegalStateException) {
                                            //The fragment is destroyed
                                        }
                                    }

                                    isDecoding = false
                                }

                                override fun onFailure(e: Exception, timeRequired: Long) {
                                    isDecoding = false
                                    e.printStackTrace()
                                    mHandler.post {
                                        if (camera2MLKitFragmentListener != null) {
                                            camera2MLKitFragmentListener!!.onError()
                                        }
                                    }
                                }
                            })
                    ocrRecognizeMlKitAsyncTask.execute()


                }
            }
        })*/

        val callbackFrameProcessor = object : FrameProcessor {
            private var isDecoding = false
            override fun invoke(frame: Frame) {
                if (!isDecoding) {
                    isDecoding = true
                    val ocrRecognizeMlKitAsyncTask = OcrRecognizeMlKitAsyncTask2(context!!.applicationContext,
                            frameProcessor!!,
                            frame!!,
                            object : VisionProcessorBase.OcrListener {
                                override fun onMRZRead(mrzInfo: MRZInfo, timeRequired: Long) {
                                    mHandler.post {
                                        try {
                                            mStatusRead!!.text = getString(R.string.status_bar_ocr, mrzInfo.documentNumber, mrzInfo.dateOfBirth, mrzInfo.dateOfExpiry)
                                            mStatusBar!!.text = getString(R.string.status_bar_success, timeRequired)
                                            mStatusBar!!.setTextColor(resources.getColor(R.color.status_text))
                                            if (camera2MLKitFragmentListener != null) {
                                                camera2MLKitFragmentListener!!.onPassportRead(mrzInfo)
                                            }

                                        } catch (e: IllegalStateException) {
                                            //The fragment is destroyed
                                        }
                                    }
                                }

                                override fun onMRZReadFailure(timeRequired: Long) {
                                    mHandler.post {
                                        try {
                                            mStatusBar!!.text = getString(R.string.status_bar_failure, timeRequired)
                                            mStatusBar!!.setTextColor(Color.RED)
                                            mStatusRead!!.text = ""
                                        } catch (e: IllegalStateException) {
                                            //The fragment is destroyed
                                        }
                                    }

                                    isDecoding = false
                                }

                                override fun onFailure(e: Exception, timeRequired: Long) {
                                    isDecoding = false
                                    e.printStackTrace()
                                    mHandler.post {
                                        if (camera2MLKitFragmentListener != null) {
                                            camera2MLKitFragmentListener!!.onError()
                                        }
                                    }
                                }
                            })
                    ocrRecognizeMlKitAsyncTask.execute()
                }
            }


        }

        fotoapparat = Fotoapparat
                .with(context!!)
                .into(camera_view)
                .frameProcessor(
                        callbackFrameProcessor
                )
                .build()

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        MRZUtil.cleanStorage()

        fotoapparat?.start()
    }



    override fun onPause() {
        fotoapparat?.stop()

        super.onPause()
    }

    override fun onDestroyView() {
        frameProcessor!!.stop()
        super.onDestroyView()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val activity = activity
        if (activity is Camera2MLKitFragmentListener) {
            camera2MLKitFragmentListener = activity
        }
    }

    override fun onDetach() {
        camera2MLKitFragmentListener = null
        super.onDetach()

    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Permissions
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.permission_camera_rationale))
                        .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Dialogs UI
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Shows a [Toast] on the UI thread.
     *
     * @param text The message to show
     */
    private fun showToast(text: String) {
        val activity = activity
        activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            return AlertDialog.Builder(activity)
                    .setMessage(arguments!!.getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok) { dialogInterface, i -> activity!!.finish() }
                    .create()
        }

        companion object {

            private val ARG_MESSAGE = "message"

            fun newInstance(message: String): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    class ConfirmationDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val parent = parentFragment
            return AlertDialog.Builder(activity)
                    .setMessage(R.string.permission_camera_rationale)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        parent!!.requestPermissions(arrayOf(Manifest.permission.CAMERA),
                                REQUEST_CAMERA_PERMISSION)
                    }
                    .setNegativeButton(android.R.string.cancel
                    ) { dialog, which ->
                        val activity = parent!!.activity
                        activity?.finish()
                    }
                    .create()
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Listener
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    interface Camera2MLKitFragmentListener {
        fun onPassportRead(mrzInfo: MRZInfo)
        fun onError()
    }

    companion object {

        /**
         * Tag for the [Log].
         */
        private val TAG = CameraMLKitFragment::class.java.simpleName

        private val REQUEST_CAMERA_PERMISSION = 1
        private val FRAGMENT_DIALOG = "Camera2MLKitFragment"

        fun newInstance(): CameraMLKitFragment {
            return CameraMLKitFragment()
        }
    }


}
