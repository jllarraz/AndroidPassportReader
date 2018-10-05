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

import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.tlv.TLVOutputStream;

import org.jmrtd.BACKeySpec;
import org.jmrtd.ChipAuthenticationResult;
import org.jmrtd.DESedeSecureMessagingWrapper;
import org.jmrtd.PassportService;
import org.jmrtd.TerminalAuthenticationResult;
import org.jmrtd.Util;
import org.jmrtd.cert.CVCAuthorizationTemplate;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.DG14File;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.MRZInfo;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.asynctask.NfcPassportAsyncTask;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.utils.ImageUtil;

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
            public void onPassportRead(MRZInfo personInfo, Bitmap faceImage) {
                NfcFragment.this.onPassportRead(personInfo, faceImage);
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

    protected void onPassportRead(final MRZInfo personInfo, final Bitmap faceImage){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(nfcFragmentListener!=null){
                    nfcFragmentListener.onPassportRead(personInfo, faceImage);
                }
            }
        });
    }

    public interface NfcFragmentListener{
        void onEnableNfc();
        void onDisableNfc();
        void onPassportRead(MRZInfo personInfo, Bitmap faceImage);
        void onCardException(CardServiceException cardException);
    }
}
