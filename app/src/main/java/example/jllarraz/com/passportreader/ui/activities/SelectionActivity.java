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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;


import org.jmrtd.lds.icao.MRZInfo;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.ui.fragments.SelectionFragment;

public class SelectionActivity extends AppCompatActivity implements SelectionFragment.SelectionFragmentListener {

    private static final String TAG = SelectionActivity.class.getSimpleName();
    private static final int REQUEST_MRZ=12;
    private static final int REQUEST_NFC=11;

    private static final String TAG_SELECTION_FRAGMENT = "TAG_SELECTION_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new SelectionFragment(), TAG_SELECTION_FRAGMENT)
                    .commit();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(data == null){
            data =  new Intent();
        }
        switch (requestCode){
            case REQUEST_MRZ:{
                switch (resultCode){
                    case RESULT_OK:{
                        onPassportRead((MRZInfo) data.getSerializableExtra(IntentData.KEY_MRZ_INFO));
                        break;
                    }
                    case RESULT_CANCELED:
                    default:{
                        Fragment fragmentByTag = getSupportFragmentManager().findFragmentByTag(TAG_SELECTION_FRAGMENT);
                        if(fragmentByTag instanceof SelectionFragment){
                            ((SelectionFragment)fragmentByTag).selectManualToggle();
                        }
                        break;
                    }
                }
                break;
            }
            case REQUEST_NFC:{
                Fragment fragmentByTag = getSupportFragmentManager().findFragmentByTag(TAG_SELECTION_FRAGMENT);
                if(fragmentByTag instanceof SelectionFragment){
                    ((SelectionFragment)fragmentByTag).selectManualToggle();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void test(){
        //Method to test NFC without rely into the Camera
        String TEST_LINE_1="P<IRLOSULLIVAN<<LAUREN<<<<<<<<<<<<<<<<<<<<<<";
        String TEST_LINE_2="XN50042162IRL8805049F2309154<<<<<<<<<<<<<<<6";

        MRZInfo mrzInfo = new MRZInfo(TEST_LINE_1+"\n"+TEST_LINE_2);
        onPassportRead(mrzInfo);
    }

    @Override
    public void onPassportRead(MRZInfo mrzInfo) {
        Intent intent = new Intent(this, NfcActivity.class);
        intent.putExtra(IntentData.KEY_MRZ_INFO, mrzInfo);
        startActivityForResult(intent, REQUEST_NFC);
    }

    @Override
    public void onMrzRequest() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, REQUEST_MRZ);
    }
}
