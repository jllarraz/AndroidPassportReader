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
package example.jllarraz.com.passportreader.mlkit;

import android.graphics.Bitmap;
import android.media.Image;
import android.support.annotation.NonNull;

import com.google.firebase.ml.common.FirebaseMLException;

import java.nio.ByteBuffer;

/** An inferface to process the images with different ML Kit detectors and custom image models. */
public interface VisionImageProcessor {

  /** Processes the images with the underlying machine learning models. */
  void process(@NonNull ByteBuffer data,@NonNull FrameMetadata frameMetadata, @NonNull VisionProcessorBase.OcrListener ocrListener)
      throws FirebaseMLException;

  /** Processes the bitmap images. */
  void process(@NonNull Bitmap bitmap, @NonNull VisionProcessorBase.OcrListener ocrListener);

  /** Processes the images. */
  void process(@NonNull Image bitmap, int rotation, @NonNull VisionProcessorBase.OcrListener ocrListener);

  /** Stops the underlying machine learning model and release resources. */
  void stop();
}
