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


import org.jmrtd.lds.icao.MRZInfo;

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

    private static String REGEX_OLD_PASSPORT ="[A-Z0-9<]{9}[0-9]{1}[A-Z<]{3}[0-9]{6}[0-9]{1}[FM<]{1}[0-9]{6}[0-9]{1}";
    private static String REGEX_IP_PASSPORT_LINE_1 ="\\bIP[A-Z<]{3}[A-Z0-9<]{9}[0-9]{1}";
    private static String REGEX_IP_PASSPORT_LINE_2 ="[0-9]{6}[0-9]{1}[FM<]{1}[0-9]{6}[0-9]{1}[A-Z<]{3}";

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
        Pattern patternLineOldPassportType = Pattern.compile(REGEX_OLD_PASSPORT);
        Matcher matcherLineOldPassportType = patternLineOldPassportType.matcher(fullRead);



        if(matcherLineOldPassportType.find()) {
            //Old passport format
            String line2 = matcherLineOldPassportType.group(0);
            String documentNumber = line2.substring(0, 9);
            String dateOfBirthDay = line2.substring(13, 19);
            String expirationDate = line2.substring(21, 27);

            //As O and 0 and really similar most of the countries just removed them from the passport, so for accuracy I am formatting it
            documentNumber=documentNumber.replaceAll("O", "0");


            MRZInfo mrzInfo = createDummyMrz(documentNumber, dateOfBirthDay, expirationDate);
            ocrListener.onMRZRead(mrzInfo, timeRequired);
        } else {
            //Try with the new IP passport type
            Pattern patternLineIPassportTypeLine1 = Pattern.compile(REGEX_IP_PASSPORT_LINE_1);
            Matcher matcherLineIPassportTypeLine1 = patternLineIPassportTypeLine1.matcher(fullRead);
            Pattern patternLineIPassportTypeLine2 = Pattern.compile(REGEX_IP_PASSPORT_LINE_2);
            Matcher matcherLineIPassportTypeLine2 = patternLineIPassportTypeLine2.matcher(fullRead);
            if(matcherLineIPassportTypeLine1.find() && matcherLineIPassportTypeLine2.find()){
                String line1 = matcherLineIPassportTypeLine1.group(0);
                String line2 = matcherLineIPassportTypeLine2.group(0);
                String documentNumber = line1.substring(5, 14);
                String dateOfBirthDay = line2.substring(0, 6);
                String expirationDate = line2.substring(8, 14);

                //As O and 0 and really similar most of the countries just removed them from the passport, so for accuracy I am formatting it
                documentNumber=documentNumber.replaceAll("O", "0");

                MRZInfo mrzInfo = createDummyMrz(documentNumber, dateOfBirthDay, expirationDate);
                ocrListener.onMRZRead(mrzInfo, timeRequired);
            } else {
                //No success
                ocrListener.onMRZReadFailure(timeRequired);
            }
        }


    }


    protected MRZInfo createDummyMrz(String documentNumber, String dateOfBirthDay, String expirationDate){
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
        return mrzInfo;
    }

    @Override
    protected void onFailure(@NonNull Exception e, long timeRequired,@NonNull OcrListener ocrListener) {
        Log.w(TAG, "Text detection failed." + e);
        ocrListener.onFailure(e, timeRequired);
    }
}
