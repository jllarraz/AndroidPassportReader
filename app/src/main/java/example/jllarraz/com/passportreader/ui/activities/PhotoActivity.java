package example.jllarraz.com.passportreader.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.ui.fragments.PassportPhotoFragment;

import static example.jllarraz.com.passportreader.common.IntentData.KEY_IMAGE;

public class PhotoActivity extends FragmentActivity implements PassportPhotoFragment.PassportPhotoFragmentListener{
    private static final String TAG = PhotoActivity.class.getSimpleName();

    private static final String TAG_PHOTO ="TAG_PHOTO";

    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        Intent intent = getIntent();
        if(intent.hasExtra(KEY_IMAGE)) {
            bitmap = (Bitmap) intent.getParcelableExtra(KEY_IMAGE);
        } else {
            onBackPressed();
        }

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, PassportPhotoFragment.newInstance(bitmap), TAG_PHOTO)
                    .commit();
        }
    }

    public void onResume() {
        super.onResume();

    }

    public void onPause() {
        super.onPause();

    }


}
