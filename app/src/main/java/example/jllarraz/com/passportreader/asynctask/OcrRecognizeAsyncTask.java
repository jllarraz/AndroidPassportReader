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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;

import com.googlecode.leptonica.android.Binarize;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

import java.util.ArrayList;

import example.jllarraz.com.passportreader.data.OcrResult;
import example.jllarraz.com.passportreader.data.OcrResultFailure;

/**
 * Class to send OCR requests to the OCR engine in a separate thread, send a success/failure message,
 * and dismiss the indeterminate progress dialog box. Used for non-continuous mode OCR only.
 */
public final class OcrRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean> {

  //  private static final boolean PERFORM_FISHER_THRESHOLDING = false; 
  //  private static final boolean PERFORM_OTSU_THRESHOLDING = false; 
  //  private static final boolean PERFORM_SOBEL_THRESHOLDING = false; 

  private Context context;
  private TessBaseAPI baseApi;
  private Bitmap bitmap;
  private OcrResult ocrResult;
  private long timeRequired;
  private OcrRecognizeAsyncTaskListener ocrRecognizeAsyncTaskListener;

  public OcrRecognizeAsyncTask(Context context, TessBaseAPI baseApi, Bitmap bitmap, OcrRecognizeAsyncTaskListener ocrRecognizeAsyncTaskListener) {
    this.context = context;
    this.baseApi = baseApi;
    this.bitmap = bitmap;
    this.ocrRecognizeAsyncTaskListener = ocrRecognizeAsyncTaskListener;
  }

  @Override
  protected Boolean doInBackground(Void... arg0) {
    long start = System.currentTimeMillis();

    String textResult;

    Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap));
    Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
    bitmap = WriteFile.writeBitmap(thresholdedImage);

    try {     
      baseApi.setImage(ReadFile.readBitmap(bitmap));
      textResult = baseApi.getUTF8Text();
      timeRequired = System.currentTimeMillis() - start;

      // Check for failure to recognize text
      if (textResult == null || textResult.equals("")) {
        return false;
      }
      ocrResult = new OcrResult();
      ocrResult.setWordConfidences(baseApi.wordConfidences());
      ocrResult.setMeanConfidence( baseApi.meanConfidence());
      ocrResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
      ocrResult.setTextlineBoundingBoxes(baseApi.getTextlines().getBoxRects());
      ocrResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
      ocrResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());


      String[] textResultTmpArr = textResult.split("\n");
      textResult = "";
      for (int i=0;i<textResultTmpArr.length;i++){
        if(textResultTmpArr[i].length() > 10){
          textResult += textResultTmpArr[i]+'\n';
        }
      }

      // Iterate through the results.
      final ResultIterator iterator = baseApi.getResultIterator();
      int[] lastBoundingBox;
      ArrayList<Rect> charBoxes = new ArrayList<Rect>();
      iterator.begin();
      do {
          lastBoundingBox = iterator.getBoundingBox(PageIteratorLevel.RIL_SYMBOL);
          Rect lastRectBox = new Rect(lastBoundingBox[0], lastBoundingBox[1],
                  lastBoundingBox[2], lastBoundingBox[3]);
          charBoxes.add(lastRectBox);
      } while (iterator.next(PageIteratorLevel.RIL_SYMBOL));
      iterator.delete();
      ocrResult.setCharacterBoundingBoxes(charBoxes);

    } catch (RuntimeException e) {
      Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
      e.printStackTrace();
      try {
        if(ocrRecognizeAsyncTaskListener!=null){
            ocrRecognizeAsyncTaskListener.onError();
        }
      } catch (NullPointerException e1) {
        // Continue
      }
      return false;
    }
    timeRequired = System.currentTimeMillis() - start;
    ocrResult.setBitmap(bitmap);
    ocrResult.setText(textResult);
    ocrResult.setRecognitionTimeRequired(timeRequired);
    return true;
  }

  @Override
  protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);

    if(ocrRecognizeAsyncTaskListener!=null){
      if (result) {
        ocrRecognizeAsyncTaskListener.onSuccess(ocrResult);
      } else {
        ocrRecognizeAsyncTaskListener.onFailure(new OcrResultFailure(timeRequired));
      }
    }
  }

  public interface OcrRecognizeAsyncTaskListener{
      void onError();
      void onSuccess(OcrResult ocrResult);
      void onFailure(OcrResultFailure ocrResult);
  }
}
