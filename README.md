# Passport Reader

Sample project to read Passports using MRZ or manual entry. Currently I am using Tesseract for the OCR but the performance it's not great.

I have also tried the lates ML KIT (https://firebase.google.com/docs/ml-kit/recognize-text) from google but it's struggling with "<<<".

You can use the example images stored under `examples` to test the application.

![alt text](https://github.com/jllarraz/AndroidPassportReader/blob/master/examples/passport_ireland.jpg)

or download any sample passport document from https://www.consilium.europa.eu/prado/EN/prado-start-page.html

This project is based in the information and tutorials found in

- https://developer.android.com/reference/android/hardware/camera2/package-summary
- https://github.com/tananaev/passport-reader/blob/master/app/build.gradle
- https://techblog.bozho.net/how-to-read-your-passport-with-android/
- https://github.com/mercuriete/android-mrz-reader
- https://en.wikipedia.org/wiki/Machine-readable_passport
- https://jmrtd.org/about.shtml


## Build & Run

```
    1. Clone Repository
    2. Open with Android Studio
    3. Configure Android SDK
    4. Launch application
```

## Things to improve

OCR must be improved to allow good user experience

## License

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

