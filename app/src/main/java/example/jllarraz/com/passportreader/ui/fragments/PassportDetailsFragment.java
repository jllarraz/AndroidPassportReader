package example.jllarraz.com.passportreader.ui.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import org.jmrtd.lds.icao.MRZInfo;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;

public class PassportDetailsFragment extends Fragment{

    private PassportDetailsFragmentListener passportDetailsFragmentListener;

    private MRZInfo mrzInfo;
    private Bitmap faceImage;

    private AppCompatImageView appCompatImageViewFace;
    private TextView textViewName;
    private TextView textViewDateOfBirth;
    private TextView textViewGender;

    private TextView textViewDocumentNumber;
    private TextView textViewExpiration;
    private TextView textViewIssuingCountry;
    private TextView textViewNationality;

    public static PassportDetailsFragment newInstance(MRZInfo mrzInfo, Bitmap face) {
        PassportDetailsFragment myFragment = new PassportDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(IntentData.KEY_MRZ_INFO, mrzInfo);
        args.putParcelable(IntentData.KEY_FACE_IMAGE, face);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View inflatedView = inflater.inflate(R.layout.fragment_passport_details, container, false);

        Bundle arguments = getArguments();
        if(arguments.containsKey(IntentData.KEY_MRZ_INFO)){
            mrzInfo = (MRZInfo) arguments.getSerializable(IntentData.KEY_MRZ_INFO);
        } else {
            //error
        }

        if(arguments.containsKey(IntentData.KEY_FACE_IMAGE)){
            faceImage = (Bitmap) arguments.getParcelable(IntentData.KEY_FACE_IMAGE);
        }

        appCompatImageViewFace =  inflatedView.findViewById(R.id.iconPhoto);
        textViewName =  inflatedView.findViewById(R.id.value_name);
        textViewDateOfBirth =  inflatedView.findViewById(R.id.value_DOB);
        textViewGender =  inflatedView.findViewById(R.id.value_gender);
        textViewDocumentNumber =  inflatedView.findViewById(R.id.value_passport_number);
        textViewExpiration =  inflatedView.findViewById(R.id.value_expiration_date);
        textViewIssuingCountry =  inflatedView.findViewById(R.id.value_issuing_state);
        textViewNationality =  inflatedView.findViewById(R.id.value_nationality);

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshData(mrzInfo, faceImage);
    }

    private void refreshData(MRZInfo mrzInfo, Bitmap face){
        if(mrzInfo == null){
            return;
        }

        if(face!=null) {
            appCompatImageViewFace.setImageBitmap(face);
        }

        String name = mrzInfo.getPrimaryIdentifier().replace("<", "");
        String surname = mrzInfo.getSecondaryIdentifier().replace("<", "");
        textViewName.setText(getString(R.string.name, name, surname));
        textViewDateOfBirth.setText(mrzInfo.getDateOfBirth());
        textViewGender.setText(mrzInfo.getGender().name());
        textViewDocumentNumber.setText(mrzInfo.getDocumentNumber());
        textViewExpiration.setText(mrzInfo.getDateOfExpiry());
        textViewIssuingCountry.setText(mrzInfo.getIssuingState());
        textViewNationality.setText(mrzInfo.getNationality());

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentActivity activity = getActivity();
        if(activity instanceof PassportDetailsFragment.PassportDetailsFragmentListener){
            passportDetailsFragmentListener = (PassportDetailsFragment.PassportDetailsFragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        passportDetailsFragmentListener = null;
        super.onDetach();

    }

    public interface PassportDetailsFragmentListener{

    }
}
