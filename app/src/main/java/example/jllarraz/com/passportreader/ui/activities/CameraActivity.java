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

package example.jllarraz.com.passportreader.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.jmrtd.lds.MRZInfo;
import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.ui.fragments.Camera2MLKitFragment;

public class CameraActivity extends AppCompatActivity implements Camera2MLKitFragment.Camera2MLKitFragmentListener {

    private static final String TAG = CameraActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new Camera2MLKitFragment())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onPassportRead(MRZInfo mrzInfo) {
        Intent intent = new Intent();
        intent.putExtra(IntentData.KEY_MRZ_INFO, mrzInfo);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onError() {
        onBackPressed();
    }
}
