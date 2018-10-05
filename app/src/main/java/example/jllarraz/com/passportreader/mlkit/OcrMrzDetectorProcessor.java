/*
 * Copyright (C) The Android Open Source Project
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
package example.jllarraz.com.passportreader.mlkit;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import net.sf.scuba.data.Gender;

import org.jmrtd.lds.MRZInfo;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrMrzDetectorProcessor extends VisionProcessorBase<FirebaseVisionText> {

    private static final String TAG = OcrMrzDetectorProcessor.class.getSimpleName();

    private static String REGEX="[A-Z0-9<]{9}[0-9]{1}[A-Z<]{3}[0-9]{6}[0-9]{1}[FM<]{1}[0-9]{6}[0-9]{1}";

    private final FirebaseVisionTextRecognizer detector;

    public OcrMrzDetectorProcessor() {
        detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }


    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
        }
    }

    @Override
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        return detector.processImage(image);
    }


    @Override
    protected void onSuccess(
            @NonNull FirebaseVisionText results,
            @NonNull FrameMetadata frameMetadata,
            long timeRequired,
            @NonNull OcrListener ocrListener) {

        String fullRead="";
        List<FirebaseVisionText.TextBlock> blocks = results.getTextBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            String temp = "";
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                //extract scanned text lines here
                //temp+=lines.get(j).getText().trim()+"-";
                temp += lines.get(j).getText() + "-";
            }
            temp = temp.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\t", "");
            fullRead += temp + "-";
        }
        Log.d(TAG, "Read: "+fullRead);
        Pattern patternLine2 = Pattern.compile(REGEX);
        Matcher matcherLine2 = patternLine2.matcher(fullRead);
        if(matcherLine2.find()) {
            String group = matcherLine2.group(0);
            String documentNumber = group.substring(0, 9);
            String dateOfBirthDay = group.substring(13, 19);
            String expirationDate = group.substring(21, 27);


            MRZInfo mrzInfo = new MRZInfo(
                    "P",
                    "ESP",
                    "DUMMY",
                    "DUMMY",
                    documentNumber,
                    "ESP",
                    dateOfBirthDay,
                    Gender.MALE,
                    expirationDate,
                    ""
            );
            ocrListener.onMRZRead(mrzInfo, timeRequired);
        } else {
            ocrListener.onMRZReadFailure(timeRequired);
        }


    }

    @Override
    protected void onFailure(@NonNull Exception e, long timeRequired,@NonNull OcrListener ocrListener) {
        Log.w(TAG, "Text detection failed." + e);
        ocrListener.onFailure(e, timeRequired);
    }
}
