/*
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.jllarraz.com.passportreader.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Installs the language data required for OCR, and initializes the OCR engine using a background 
 * thread.
 */
public class OcrInitAsyncTask extends AsyncTask<String, String, Boolean> {
  private static final String TAG = OcrInitAsyncTask.class.getSimpleName();

  private TessBaseAPI baseApi;
  private final String languageCode;
  private String languageName;
  private int ocrEngineMode;


  private Context mContext;
  private OcrInitAsyncTaskListener ocrInitAsyncTaskListener;

  /**
   * AsyncTask to asynchronously download data and initialize Tesseract.
   * 
   * @param context
   *          The android context
   * @param baseApi
   *          API to the OCR engine
   * @param languageCode
   *          ISO 639-2 OCR language code
   * @param languageName
   *          Name of the OCR language, for example, "English"
   * @param ocrEngineMode
   *          Whether to use Tesseract, Cube, or both
   * @param ocrInitAsyncTaskListener
   *          Callback to report events
   */
  public OcrInitAsyncTask(Context context, TessBaseAPI baseApi, String languageCode, String languageName,
                   int ocrEngineMode, OcrInitAsyncTaskListener ocrInitAsyncTaskListener) {
    this.baseApi = baseApi;
    this.languageCode = languageCode;
    this.languageName = languageName;
    this.ocrEngineMode = ocrEngineMode;
    this.mContext = context;
    this.ocrInitAsyncTaskListener = ocrInitAsyncTaskListener;

  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    if(ocrInitAsyncTaskListener!=null){
      ocrInitAsyncTaskListener.onStart();
    }
  }

  /**
   * In background thread, perform required setup, and request initialization of
   * the OCR engine.
   * 
   * @param params
   *          [0] Pathname for the directory for storing language data files to the SD card
   */
  protected Boolean doInBackground(String... params) {

    //File f = new File(this.mContext.getCacheDir()+"/tessdata/eng.traineddata");
    File f = new File(this.mContext.getCacheDir()+"/tessdata/eng.traineddata");
    File folder = new File(this.mContext.getCacheDir()+"/tessdata");
    if (!f.exists()) try {
        if (!f.exists() && !folder.mkdirs()) {
            Log.e(TAG, "Couldn't make directory " + folder);
            return false;
        }
        InputStream is = this.mContext.getAssets().open("tessdata/eng.traineddata");
        FileOutputStream fos = new FileOutputStream(f);
        copyFile(is,fos);
        fos.close();
        is.close();
    } catch (Exception e) { throw new RuntimeException(e); }




    File f2 = new File(this.mContext.getCacheDir()+"/tessdata/eng.user-patterns");
    if (!f2.exists()) try {

      InputStream is = this.mContext.getAssets().open("tessdata/eng.user-patterns");
      FileOutputStream fos = new FileOutputStream(f2);
      copyFile(is,fos);
      fos.close();
      is.close();
    } catch (Exception e) { throw new RuntimeException(e); }
      // Initialize the OCR engine
    if (baseApi.init(this.mContext.getCacheDir() + File.separator, languageCode, ocrEngineMode)) {
      return true;
    }
    return false;
  }


  private boolean copyFile(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int read;
    while((read = in.read(buffer)) != -1){
      out.write(buffer, 0, read);
    }
    return true;
  }


  /**
   * Update the dialog box with the latest incremental progress.
   * 
   * @param message
   *          [0] Text to be displayed
   * @param message
   *          [1] Numeric value for the progress
   */
  @Override
  protected void onProgressUpdate(String... message) {
    super.onProgressUpdate(message);
    int percentComplete = 0;

    percentComplete = Integer.parseInt(message[1]);
    if(ocrInitAsyncTaskListener!=null){
      ocrInitAsyncTaskListener.onProgress(percentComplete);
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);
    if(ocrInitAsyncTaskListener!=null) {
      if (result) {
        ocrInitAsyncTaskListener.onSuccess();
      } else {
        ocrInitAsyncTaskListener.onError();
      }
    }
  }

  public interface OcrInitAsyncTaskListener{
      void onError();
      void onSuccess();
      void onProgress(int percentage);
      void onStart();
  }

}