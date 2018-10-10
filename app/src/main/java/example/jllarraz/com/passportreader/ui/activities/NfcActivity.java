package example.jllarraz.com.passportreader.ui.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import net.sf.scuba.smartcards.CardServiceException;


import org.jmrtd.lds.icao.MRZInfo;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.data.Passport;
import example.jllarraz.com.passportreader.ui.fragments.NfcFragment;
import example.jllarraz.com.passportreader.ui.fragments.PassportDetailsFragment;

import static example.jllarraz.com.passportreader.common.IntentData.KEY_MRZ_INFO;

public class NfcActivity extends FragmentActivity implements NfcFragment.NfcFragmentListener, PassportDetailsFragment.PassportDetailsFragmentListener {

    private static final String TAG = NfcActivity.class.getSimpleName();

    private static final int REQUEST_VIEW_PICTURE=123;

    private MRZInfo mrzInfo = null;

    private static final String TAG_NFC="TAG_NFC";
    private static final String TAG_PASSPORT_DETAILS="TAG_PASSPORT_DETAILS";

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        Intent intent = getIntent();
        if(intent.hasExtra(KEY_MRZ_INFO)) {
            mrzInfo = (MRZInfo) intent.getSerializableExtra(KEY_MRZ_INFO);
        } else {
            onBackPressed();
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, getString(R.string.warning_no_nfc), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, NfcFragment.newInstance(mrzInfo), TAG_NFC)
                    .commit();
        }
    }

    public void onResume() {
        super.onResume();

    }

    public void onPause() {
        super.onPause();

    }

    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            // drop NFC events
            handleIntent(intent);
        }
    }

    protected void handleIntent(Intent intent){
        Fragment fragmentByTag = getSupportFragmentManager().findFragmentByTag(TAG_NFC);
        if(fragmentByTag instanceof NfcFragment){
            ((NfcFragment)fragmentByTag).handleNfcTag(intent);
        }
    }


    /////////////////////////////////////////////////////
    //
    //  NFC Fragment events
    //
    /////////////////////////////////////////////////////

    @Override
    public void onEnableNfc() {


        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled())
                showWirelessSettings();

            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    public void onDisableNfc() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onPassportRead(Passport passport) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, PassportDetailsFragment.newInstance(passport), TAG_PASSPORT_DETAILS)
                .commit();
    }

    @Override
    public void onCardException(CardServiceException cardException) {
        Toast.makeText(this, cardException.toString(), Toast.LENGTH_SHORT).show();
        //onBackPressed();
    }

    private void showWirelessSettings() {
        Toast.makeText(this, getString(R.string.warning_enable_nfc), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onImageSelected(Bitmap bitmap) {
        Intent intentRequest = new Intent(this, PhotoActivity.class);
        intentRequest.putExtra(IntentData.KEY_IMAGE, bitmap);
        startActivityForResult(intentRequest, REQUEST_VIEW_PICTURE);

    }
}
