// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package example.jllarraz.com.passportreader.mlkit

import android.graphics.Bitmap
import android.media.Image

import com.google.firebase.ml.common.FirebaseMLException
import io.fotoapparat.preview.Frame

import java.nio.ByteBuffer

/** An inferface to process the images with different ML Kit detectors and custom image models.  */
interface VisionImageProcessor {

    /** Processes the images with the underlying machine learning models.  */
    @Throws(FirebaseMLException::class)
    fun process(data: ByteBuffer, frameMetadata: FrameMetadata, ocrListener: VisionProcessorBase.OcrListener)

    /** Processes the bitmap images.  */
    fun process(bitmap: Bitmap, ocrListener: VisionProcessorBase.OcrListener)

    /** Processes the bitmap images.  */
    fun process(frame:Frame, ocrListener: VisionProcessorBase.OcrListener)

    /** Processes the images.  */
    fun process(bitmap: Image, rotation: Int, ocrListener: VisionProcessorBase.OcrListener)

    /** Stops the underlying machine learning model and release resources.  */
    fun stop()
}
