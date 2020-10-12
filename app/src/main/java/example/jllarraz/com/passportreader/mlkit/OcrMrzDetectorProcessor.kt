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
package example.jllarraz.com.passportreader.mlkit

import android.graphics.Bitmap
import android.util.Log

import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer

import net.sf.scuba.data.Gender


import org.jmrtd.lds.icao.MRZInfo

import java.io.IOException
import java.util.regex.Pattern


/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
class OcrMrzDetectorProcessor(val callback: MRZCallback) : VisionProcessorBase<FirebaseVisionText>() {

    private val detector: FirebaseVisionTextRecognizer

    init {
        detector = FirebaseVision.getInstance().onDeviceTextRecognizer
    }


    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: $e")
        }

    }

    override fun detectInImage(image: FirebaseVisionImage): Task<FirebaseVisionText> {
        return detector.processImage(image)
    }


    override fun onSuccess(
            results: FirebaseVisionText,
            frameMetadata: FrameMetadata?,
            timeRequired: Long,
            bitmap: Bitmap
            ) {

        var fullRead = ""
        val blocks = results.textBlocks
        for (i in blocks.indices) {
            var temp = ""
            val lines = blocks[i].lines
            for (j in lines.indices) {
                //extract scanned text lines here
                //temp+=lines.get(j).getText().trim()+"-";
                temp += lines[j].text + "-"
            }
            temp = temp.replace("\r".toRegex(), "").replace("\n".toRegex(), "").replace("\t".toRegex(), "").replace(" ", "")
            fullRead += "$temp-"
        }
        fullRead = fullRead.toUpperCase()
        Log.d(TAG, "Read: $fullRead")
        val patternLineOldPassportType = Pattern.compile(REGEX_OLD_PASSPORT)
        val matcherLineOldPassportType = patternLineOldPassportType.matcher(fullRead)



        if (matcherLineOldPassportType.find()) {
            //Old passport format
            val line2 = matcherLineOldPassportType.group(0)
            var documentNumber = matcherLineOldPassportType.group(1)
            val dateOfBirthDay = cleanDate(matcherLineOldPassportType.group(4))
            val expirationDate = cleanDate(matcherLineOldPassportType.group(7))

            //As O and 0 and really similar most of the countries just removed them from the passport, so for accuracy I am formatting it
            documentNumber = documentNumber.replace("O".toRegex(), "0")


            val mrzInfo = createDummyMrz(documentNumber, dateOfBirthDay, expirationDate)
            callback.onMRZRead(mrzInfo, timeRequired)
        } else {
            //Try with the new IP passport type
            val patternLineIPassportTypeLine1 = Pattern.compile(REGEX_IP_PASSPORT_LINE_1)
            val matcherLineIPassportTypeLine1 = patternLineIPassportTypeLine1.matcher(fullRead)
            val patternLineIPassportTypeLine2 = Pattern.compile(REGEX_IP_PASSPORT_LINE_2)
            val matcherLineIPassportTypeLine2 = patternLineIPassportTypeLine2.matcher(fullRead)
            if (matcherLineIPassportTypeLine1.find() && matcherLineIPassportTypeLine2.find()) {
                val line1 = matcherLineIPassportTypeLine1.group(0)
                val line2 = matcherLineIPassportTypeLine2.group(0)
                var documentNumber = line1.substring(5, 14)
                val dateOfBirthDay = line2.substring(0, 6)
                val expirationDate = line2.substring(8, 14)

                //As O and 0 and really similar most of the countries just removed them from the passport, so for accuracy I am formatting it
                documentNumber = documentNumber.replace("O".toRegex(), "0")

                val mrzInfo = createDummyMrz(documentNumber, dateOfBirthDay, expirationDate)
                callback.onMRZRead(mrzInfo, timeRequired)
            } else {
                //No success
                callback.onMRZReadFailure(timeRequired)
            }
        }


    }


    protected fun createDummyMrz(documentNumber: String, dateOfBirthDay: String, expirationDate: String): MRZInfo {
        return MRZInfo(
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
        )
    }

    private fun cleanDate(date:String):String{
        var tempDate = date
        tempDate = tempDate.replace("I".toRegex(), "1")
        tempDate = tempDate.replace("L".toRegex(), "1")
        tempDate = tempDate.replace("D".toRegex(), "0")
        tempDate = tempDate.replace("O".toRegex(), "0")
        tempDate = tempDate.replace("S".toRegex(), "5")
        tempDate = tempDate.replace("G".toRegex(), "6")
        return tempDate
    }

    override fun onFailure(e: Exception, timeRequired: Long) {
        Log.w(TAG, "Text detection failed.$e")
        callback.onFailure(e, timeRequired)
    }

    interface MRZCallback {
        fun onMRZRead(mrzInfo: MRZInfo, timeRequired: Long)
        fun onMRZReadFailure(timeRequired: Long)
        fun onFailure(e: Exception, timeRequired: Long)
    }

    companion object {

        private val TAG = OcrMrzDetectorProcessor::class.java.simpleName

        private val REGEX_OLD_PASSPORT = "(?<documentNumber>[A-Z0-9<]{9})(?<checkDigitDocumentNumber>[0-9ILDSOG]{1})(?<nationality>[A-Z<]{3})(?<dateOfBirth>[0-9ILDSOG]{6})(?<checkDigitDateOfBirth>[0-9ILDSOG]{1})(?<sex>[FM<]){1}(?<expirationDate>[0-9ILDSOG]{6})(?<checkDigitExpiration>[0-9ILDSOG]{1})"
        private val REGEX_OLD_PASSPORT_CLEAN = "(?<documentNumber>[A-Z0-9<]{9})(?<checkDigitDocumentNumber>[0-9]{1})(?<nationality>[A-Z<]{3})(?<dateOfBirth>[0-9]{6})(?<checkDigitDateOfBirth>[0-9]{1})(?<sex>[FM<]){1}(?<expirationDate>[0-9]{6})(?<checkDigitExpiration>[0-9]{1})"
        private val REGEX_IP_PASSPORT_LINE_1 = "\\bIP[A-Z<]{3}[A-Z0-9<]{9}[0-9]{1}"
        private val REGEX_IP_PASSPORT_LINE_2 = "[0-9]{6}[0-9]{1}[FM<]{1}[0-9]{6}[0-9]{1}[A-Z<]{3}"
    }
}
