package example.jllarraz.com.passportreader.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.data.AdditionalPersonDetails;
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

    private TextView textViewAdditionalInfoCustody;
    private TextView textViewAdditionalDateOfBirth;
    private TextView textViewAdditionalOtherNames;
    private TextView textViewAdditionalOtherTdNumbers;
    private TextView textViewAdditionalPermanentAddress;
    private TextView textViewAdditionalPersonalNumber;
    private TextView textViewAdditionalPersonalSummary;
    private TextView textViewAdditionalPlaceOfBirth;
    private TextView textViewAdditionalProfession;
    private TextView textViewAdditionalTelephone;
    private TextView textViewAdditionalTitle;

    private ImageView imageViewChipAuthentication;
    private ImageView imageViewTerminalAuthentication;


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

        textViewAdditionalInfoCustody =  inflatedView.findViewById(R.id.value_custody);
        textViewAdditionalDateOfBirth =  inflatedView.findViewById(R.id.value_date_of_birth);
        textViewAdditionalOtherNames =  inflatedView.findViewById(R.id.value_other_names);
        textViewAdditionalOtherTdNumbers =  inflatedView.findViewById(R.id.value_other_td_numbers);
        textViewAdditionalPermanentAddress =  inflatedView.findViewById(R.id.value_permanent_address);
        textViewAdditionalPersonalNumber =  inflatedView.findViewById(R.id.value_personal_number);
        textViewAdditionalPersonalSummary =  inflatedView.findViewById(R.id.value_personal_summary);
        textViewAdditionalPlaceOfBirth =  inflatedView.findViewById(R.id.value_place_of_birth);
        textViewAdditionalProfession =  inflatedView.findViewById(R.id.value_profession);
        textViewAdditionalTelephone =  inflatedView.findViewById(R.id.value_telephone);
        textViewAdditionalTitle =  inflatedView.findViewById(R.id.value_title);

        imageViewChipAuthentication =  inflatedView.findViewById(R.id.value_chip_authentication);
        imageViewTerminalAuthentication =  inflatedView.findViewById(R.id.value_terminal_authentication);


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
            //Add teh face
            appCompatImageViewFace.setImageBitmap(passport.getFace());
        } else if(passport.getPortrait()!=null){
            //If we don't have the face, we try with the portrait
            appCompatImageViewFace.setImageBitmap(passport.getPortrait());
        }

        PersonDetails personDetails = passport.getPersonDetails();
        if(personDetails!=null) {
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

        AdditionalPersonDetails additionalPersonDetails = passport.getAdditionalPersonDetails();
        if(additionalPersonDetails!=null){
            //This object it's not available in the majority of passports

            if(additionalPersonDetails.getCustodyInformation()!=null) {
                textViewAdditionalInfoCustody.setText(additionalPersonDetails.getCustodyInformation());
            }
            if(additionalPersonDetails.getFullDateOfBirth()!=null) {
                SimpleDateFormat simpleDateFormat =  new SimpleDateFormat("dd/MM/yyyy");
                textViewAdditionalDateOfBirth.setText(simpleDateFormat.format(additionalPersonDetails.getFullDateOfBirth()));
            }
            if(additionalPersonDetails.getOtherNames()!=null && additionalPersonDetails.getOtherNames().size()>0) {
                textViewAdditionalOtherNames.setText(arrayToString(additionalPersonDetails.getOtherNames()));
            }
            if(additionalPersonDetails.getOtherValidTDNumbers()!=null && additionalPersonDetails.getOtherValidTDNumbers().size()>0) {
                textViewAdditionalOtherTdNumbers.setText(arrayToString(additionalPersonDetails.getOtherValidTDNumbers()));
            }
            if(additionalPersonDetails.getPermanentAddress()!=null && additionalPersonDetails.getPermanentAddress().size()>0) {
                textViewAdditionalPermanentAddress.setText(arrayToString(additionalPersonDetails.getPermanentAddress()));
            }

            if(additionalPersonDetails.getPersonalNumber()!=null) {
                textViewAdditionalPersonalNumber.setText(additionalPersonDetails.getPersonalNumber());
            }

            if(additionalPersonDetails.getPersonalSummary()!=null) {
                textViewAdditionalPersonalSummary.setText(additionalPersonDetails.getPersonalSummary());
            }

            if(additionalPersonDetails.getPlaceOfBirth()!=null && additionalPersonDetails.getPlaceOfBirth().size()>0) {
                textViewAdditionalPlaceOfBirth.setText(arrayToString(additionalPersonDetails.getPlaceOfBirth()));
            }

            if(additionalPersonDetails.getProfession()!=null) {
                textViewAdditionalProfession.setText(additionalPersonDetails.getProfession());
            }

            if(additionalPersonDetails.getTelephone()!=null) {
                textViewAdditionalTelephone.setText(additionalPersonDetails.getTelephone());
            }

            if(additionalPersonDetails.getTitle()!=null) {
                textViewAdditionalTitle.setText(additionalPersonDetails.getTitle());
            }
        }

        if(passport.isChipAuthentication()){
            imageViewChipAuthentication.setImageResource(R.drawable.ic_check_circle_outline);
            imageViewChipAuthentication.setColorFilter(ContextCompat.getColor(getActivity(), android.R.color.holo_green_light), android.graphics.PorterDuff.Mode.SRC_IN);
        }else{
            imageViewChipAuthentication.setImageResource(R.drawable.ic_close_circle_outline);
            imageViewChipAuthentication.setColorFilter(ContextCompat.getColor(getActivity(), android.R.color.holo_red_light), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        if(passport.isEAC()){
            imageViewTerminalAuthentication.setImageResource(R.drawable.ic_check_circle_outline);
            imageViewTerminalAuthentication.setColorFilter(ContextCompat.getColor(getActivity(), android.R.color.holo_green_light), android.graphics.PorterDuff.Mode.SRC_IN);
        }else{
            imageViewTerminalAuthentication.setImageResource(R.drawable.ic_close_circle_outline);
            imageViewTerminalAuthentication.setColorFilter(ContextCompat.getColor(getActivity(), android.R.color.holo_red_light), android.graphics.PorterDuff.Mode.SRC_IN);
        }


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


    private String arrayToString(List<String> array){
        String temp="";
        Iterator<String> iterator = array.iterator();
        while (iterator.hasNext()){
            temp+=iterator.next()+"\n";
        }
        if(temp.endsWith("\n")){
            temp=temp.substring(0, temp.length()-"\n".length());
        }
        return temp;
    }
}
