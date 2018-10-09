package example.jllarraz.com.passportreader.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.sf.scuba.smartcards.CardServiceException;


import org.jmrtd.lds.icao.MRZInfo;
import org.spongycastle.jce.provider.BouncyCastleProvider;


import java.security.Security;


import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.asynctask.NfcPassportAsyncTask;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.data.Passport;


public class NfcFragment extends Fragment {
    private static final String TAG = NfcFragment.class.getSimpleName();

    private MRZInfo mrzInfo;
    private NfcFragmentListener nfcFragmentListener;
    private TextView textViewPassportNumber;
    private TextView textViewDateOfBirth;
    private TextView textViewDateOfExpiry;
    private ProgressBar progressBar;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    Handler mHandler = new Handler(Looper.getMainLooper());

    public static NfcFragment newInstance(MRZInfo mrzInfo) {
        NfcFragment myFragment = new NfcFragment();
        Bundle args = new Bundle();
        args.putSerializable(IntentData.KEY_MRZ_INFO, mrzInfo);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View inflatedView = inflater.inflate(R.layout.fragment_nfc, container, false);
        Bundle arguments = getArguments();
        if(arguments.containsKey(IntentData.KEY_MRZ_INFO)){
            mrzInfo = (MRZInfo) arguments.getSerializable(IntentData.KEY_MRZ_INFO);
        } else {
            //error
        }

        textViewPassportNumber = inflatedView.findViewById(R.id.value_passport_number);
        textViewDateOfBirth = inflatedView.findViewById(R.id.value_DOB);
        textViewDateOfExpiry = inflatedView.findViewById(R.id.value_expiration_date);
        progressBar = inflatedView.findViewById(R.id.progressBar);
        return inflatedView;
    }

    public void handleNfcTag(Intent intent){
        if (intent == null || intent.getExtras() == null) {
            return;
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }
        onNFCSReadStart();
        NfcPassportAsyncTask nfcPassportAsyncTask = new NfcPassportAsyncTask(getContext().getApplicationContext(), tag, mrzInfo, new NfcPassportAsyncTask.NfcPassportAsyncTaskListener() {
            @Override
            public void onPassportRead(Passport passport) {
                NfcFragment.this.onPassportRead(passport);
                onNFCReadFinish();
            }

            @Override
            public void onCardException(CardServiceException cardException) {
                NfcFragment.this.onCardException(cardException);
                onNFCReadFinish();
            }
        });
        nfcPassportAsyncTask.execute();

    }




    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if(activity instanceof NfcFragment.NfcFragmentListener){
            nfcFragmentListener = (NfcFragment.NfcFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        nfcFragmentListener = null;
        super.onDetach();
    }


    @Override
    public void onResume() {
        super.onResume();

        textViewPassportNumber.setText(getString(R.string.doc_number, mrzInfo.getDocumentNumber()));
        textViewDateOfBirth.setText(getString(R.string.doc_dob, mrzInfo.getDateOfBirth()));
        textViewDateOfExpiry.setText(getString(R.string.doc_expiry, mrzInfo.getDateOfExpiry()));

        if(nfcFragmentListener!=null){
            nfcFragmentListener.onEnableNfc();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(nfcFragmentListener!=null){
            nfcFragmentListener.onDisableNfc();
        }
    }

    protected void onNFCSReadStart(){
        Log.d(TAG, "onNFCSReadStart");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });

    }

    protected void onNFCReadFinish(){
        Log.d(TAG, "onNFCReadFinish");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    protected void onCardException(CardServiceException cardException){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(nfcFragmentListener!=null){
                    nfcFragmentListener.onCardException(cardException);
                }
            }
        });
    }

    protected void onPassportRead(final Passport passport){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(nfcFragmentListener!=null){
                    nfcFragmentListener.onPassportRead(passport);
                }
            }
        });
    }

    public interface NfcFragmentListener{
        void onEnableNfc();
        void onDisableNfc();
        void onPassportRead(Passport passport);
        void onCardException(CardServiceException cardException);
    }
}
