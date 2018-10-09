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
import example.jllarraz.com.passportreader.data.Passport;
import example.jllarraz.com.passportreader.data.PersonDetails;

public class PassportDetailsFragment extends Fragment{

    private PassportDetailsFragmentListener passportDetailsFragmentListener;

    private Passport passport;

    private AppCompatImageView appCompatImageViewFace;
    private TextView textViewName;
    private TextView textViewDateOfBirth;
    private TextView textViewGender;

    private TextView textViewDocumentNumber;
    private TextView textViewExpiration;
    private TextView textViewIssuingCountry;
    private TextView textViewNationality;

    public static PassportDetailsFragment newInstance(Passport passport) {
        PassportDetailsFragment myFragment = new PassportDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(IntentData.KEY_PASSPORT, passport);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View inflatedView = inflater.inflate(R.layout.fragment_passport_details, container, false);

        Bundle arguments = getArguments();
        if(arguments.containsKey(IntentData.KEY_PASSPORT)){
            passport = (Passport) arguments.getParcelable(IntentData.KEY_PASSPORT);
        } else {
            //error
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

        refreshData(passport);
    }

    private void refreshData(Passport passport){
        if(passport == null){
            return;
        }

        if(passport.getFace()!=null) {
            appCompatImageViewFace.setImageBitmap(passport.getFace());
        }

        PersonDetails personDetails = passport.getPersonDetails();

        String name = personDetails.getPrimaryIdentifier().replace("<", "");
        String surname = personDetails.getSecondaryIdentifier().replace("<", "");
        textViewName.setText(getString(R.string.name, name, surname));
        textViewDateOfBirth.setText(personDetails.getDateOfBirth());
        textViewGender.setText(personDetails.getGender().name());
        textViewDocumentNumber.setText(personDetails.getDocumentNumber());
        textViewExpiration.setText(personDetails.getDateOfExpiry());
        textViewIssuingCountry.setText(personDetails.getIssuingState());
        textViewNationality.setText(personDetails.getNationality());

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
