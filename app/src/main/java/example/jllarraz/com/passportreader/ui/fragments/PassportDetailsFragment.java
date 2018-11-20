package example.jllarraz.com.passportreader.ui.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;


import org.apache.commons.codec.binary.Hex;
import org.jmrtd.FeatureStatus;
import org.jmrtd.VerificationStatus;
import org.jmrtd.lds.SODFile;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import butterknife.BindView;
import butterknife.ButterKnife;
import example.jllarraz.com.passportreader.R;
import example.jllarraz.com.passportreader.common.IntentData;
import example.jllarraz.com.passportreader.data.AdditionalDocumentDetails;
import example.jllarraz.com.passportreader.data.AdditionalPersonDetails;
import example.jllarraz.com.passportreader.data.Passport;
import example.jllarraz.com.passportreader.data.PersonDetails;
import example.jllarraz.com.passportreader.utils.StringUtils;

public class PassportDetailsFragment extends Fragment{

    private PassportDetailsFragmentListener passportDetailsFragmentListener;

    SimpleDateFormat simpleDateFormat =  new SimpleDateFormat("dd/MM/yyyy");

    private Passport passport;

    @BindView(R.id.iconPhoto)
    AppCompatImageView appCompatImageViewFace;

    @BindView(R.id.value_name)
    TextView textViewName;

    @BindView(R.id.value_DOB)
    TextView textViewDateOfBirth;

    @BindView(R.id.value_gender)
    TextView textViewGender;

    @BindView(R.id.value_passport_number)
    TextView textViewDocumentNumber;

    @BindView(R.id.value_expiration_date)
    TextView textViewExpiration;

    @BindView(R.id.value_issuing_state)
    TextView textViewIssuingCountry;

    @BindView(R.id.value_nationality)
    TextView textViewNationality;


    @BindView(R.id.card_view_additional_person_information)
    CardView cardViewAdditionalPersonInformation;


    @BindView(R.id.value_custody)
    TextView textViewAdditionalInfoCustody;

    @BindView(R.id.value_date_of_birth)
    TextView textViewAdditionalDateOfBirth;

    @BindView(R.id.value_other_names)
    TextView textViewAdditionalOtherNames;

    @BindView(R.id.value_other_td_numbers)
    TextView textViewAdditionalOtherTdNumbers;

    @BindView(R.id.value_permanent_address)
    TextView textViewAdditionalPermanentAddress;

    @BindView(R.id.value_personal_number)
    TextView textViewAdditionalPersonalNumber;

    @BindView(R.id.value_personal_summary)
    TextView textViewAdditionalPersonalSummary;

    @BindView(R.id.value_place_of_birth)
    TextView textViewAdditionalPlaceOfBirth;

    @BindView(R.id.value_profession)
    TextView textViewAdditionalProfession;

    @BindView(R.id.value_telephone)
    TextView textViewAdditionalTelephone;

    @BindView(R.id.value_title)
    TextView textViewAdditionalTitle;

    @BindView(R.id.value_bac)
    ImageView imageViewBAC;

    @BindView(R.id.value_pace)
    ImageView imageViewPACE;

    @BindView(R.id.value_chip)
    ImageView imageViewChip;

    @BindView(R.id.value_passive)
    ImageView imageViewPassive;

    @BindView(R.id.value_active)
    ImageView imageViewActive;

    @BindView(R.id.value_eac)
    ImageView imageViewEac;

    @BindView(R.id.value_document_signing)
    ImageView imageViewDocumentSigning;

    @BindView(R.id.row_bac)
    TableRow tableRowBac;

    @BindView(R.id.row_pace)
    TableRow tableRowPace;

    @BindView(R.id.row_chip)
    TableRow tableRowChip;

    @BindView(R.id.row_passive)
    TableRow tableRowPassive;

    @BindView(R.id.row_active)
    TableRow tableRowActive;

    @BindView(R.id.row_document_signing)
    TableRow tableRowDocumentSigning;

    @BindView(R.id.row_eac)
    TableRow tableRowEac;



    @BindView(R.id.card_view_additional_document_information)
    CardView cardViewAdditionalDocumentInformation;

    @BindView(R.id.value_endorsements)
    TextView textViewAdditionalDocumentEndorsements;

    @BindView(R.id.value_date_personalization)
    TextView textViewAdditionalDocumentDatePersonalization;

    @BindView(R.id.value_date_issue)
    TextView textViewAdditionalDocumentDateIssue;

    @BindView(R.id.value_issuing_authority)
    TextView textViewAdditionalDocumentIssuingAuthority;

    @BindView(R.id.value_names_other_persons)
    TextView textViewAdditionalDocumentOtherNames;

    @BindView(R.id.value_system_serial_number)
    TextView textViewAdditionalDocumentSystemSerialNumber;

    @BindView(R.id.value_tax_exit)
    TextView textViewAdditionalDocumentTaxOrExit;


    @BindView(R.id.card_view_document_signing_certificate)
    CardView cardViewDocumentSigningCertificate;

    @BindView(R.id.value_document_signing_certificate_serial_number)
    TextView textViewDocumentSigningCertificateSerialNumber;

    @BindView(R.id.value_document_signing_certificate_public_key_algorithm)
    TextView textViewDocumentSigningCertificatePublicKeyAlgorithm;

    @BindView(R.id.value_document_signing_certificate_signature_algorithm)
    TextView textViewDocumentSigningCertificateSignatureAlgorithm;

    @BindView(R.id.value_document_signing_certificate_thumbprint)
    TextView textViewDocumentSigningCertificateThumbprint;

    @BindView(R.id.value_document_signing_certificate_issuer)
    TextView textViewDocumentSigningCertificateIssuer;

    @BindView(R.id.value_document_signing_certificate_subject)
    TextView textViewDocumentSigningCertificateSubject;

    @BindView(R.id.value_document_signing_certificate_valid_from)
    TextView textViewDocumentSigningCertificateValidFrom;

    @BindView(R.id.value_document_signing_certificate_valid_to)
    TextView textViewDocumentSigningCertificateValidTo;





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

        ButterKnife.bind(this, inflatedView);

        Bundle arguments = getArguments();
        if(arguments.containsKey(IntentData.KEY_PASSPORT)){
            passport = (Passport) arguments.getParcelable(IntentData.KEY_PASSPORT);
        } else {
            //error
        }


        appCompatImageViewFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bitmap = passport.getFace();
                if(bitmap==null){
                    bitmap = passport.getPortrait();
                }
                if(passportDetailsFragmentListener!=null) {
                    passportDetailsFragmentListener.onImageSelected(bitmap);
                }
            }
        });


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
            cardViewAdditionalPersonInformation.setVisibility(View.VISIBLE);

            if(additionalPersonDetails.getCustodyInformation()!=null) {
                textViewAdditionalInfoCustody.setText(additionalPersonDetails.getCustodyInformation());
            }
            if(additionalPersonDetails.getFullDateOfBirth()!=null) {

                textViewAdditionalDateOfBirth.setText(additionalPersonDetails.getFullDateOfBirth());
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
        }else{
            cardViewAdditionalPersonInformation.setVisibility(View.GONE);
        }

        AdditionalDocumentDetails additionalDocumentDetails = passport.getAdditionalDocumentDetails();
        if(additionalDocumentDetails!=null){
            cardViewAdditionalDocumentInformation.setVisibility(View.VISIBLE);

            if(additionalDocumentDetails.getDateAndTimeOfPersonalization()!=null) {
                textViewAdditionalDocumentDatePersonalization.setText(additionalDocumentDetails.getDateAndTimeOfPersonalization());
            }
            if(additionalDocumentDetails.getDateOfIssue()!=null) {
                textViewAdditionalDocumentDateIssue.setText(additionalDocumentDetails.getDateOfIssue());
            }

            if(additionalDocumentDetails.getEndorsementsAndObservations()!=null) {
                textViewAdditionalDocumentEndorsements.setText(additionalDocumentDetails.getEndorsementsAndObservations());
            }

            if(additionalDocumentDetails.getEndorsementsAndObservations()!=null) {
                textViewAdditionalDocumentEndorsements.setText(additionalDocumentDetails.getEndorsementsAndObservations());
            }

            if(additionalDocumentDetails.getIssuingAuthority()!=null) {
                textViewAdditionalDocumentIssuingAuthority.setText(additionalDocumentDetails.getIssuingAuthority());
            }

            if(additionalDocumentDetails.getNamesOfOtherPersons()!=null) {
                textViewAdditionalDocumentOtherNames.setText(arrayToString(additionalDocumentDetails.getNamesOfOtherPersons()));
            }

            if(additionalDocumentDetails.getPersonalizationSystemSerialNumber()!=null) {
                textViewAdditionalDocumentSystemSerialNumber.setText(additionalDocumentDetails.getPersonalizationSystemSerialNumber());
            }

            if(additionalDocumentDetails.getTaxOrExitRequirements()!=null) {
                textViewAdditionalDocumentTaxOrExit.setText(additionalDocumentDetails.getTaxOrExitRequirements());
            }
        } else{
            cardViewAdditionalDocumentInformation.setVisibility(View.GONE);
        }

        displayAuthenticationStatus(passport.getVerificationStatus(), passport.getFeatureStatus());




        SODFile sodFile = passport.getSodFile();
        if(sodFile!=null){
            X500Principal countrySigningCertificate = sodFile.getIssuerX500Principal();
            String dnRFC2253 = countrySigningCertificate.getName(X500Principal.RFC2253);
            String dnCANONICAL = countrySigningCertificate.getName(X500Principal.CANONICAL);
            String dnRFC1779 = countrySigningCertificate.getName(X500Principal.RFC1779);

            String name = countrySigningCertificate.getName();
            //new X509Certificate(countrySigningCertificate);

            X509Certificate docSigningCertificate = sodFile.getDocSigningCertificate();

            if(docSigningCertificate !=null){

                textViewDocumentSigningCertificateSerialNumber.setText(docSigningCertificate.getSerialNumber().toString());
                textViewDocumentSigningCertificatePublicKeyAlgorithm.setText(docSigningCertificate.getPublicKey().getAlgorithm());
                textViewDocumentSigningCertificateSignatureAlgorithm.setText(docSigningCertificate.getSigAlgName());

                try {
                    textViewDocumentSigningCertificateThumbprint.setText(StringUtils.bytesToHex(MessageDigest.getInstance("SHA-1").digest(
                            docSigningCertificate.getEncoded())).toUpperCase());
                }catch (Exception e){
                    e.printStackTrace();
                }
                textViewDocumentSigningCertificateIssuer.setText(docSigningCertificate.getIssuerDN().getName());
                textViewDocumentSigningCertificateSubject.setText(docSigningCertificate.getSubjectDN().getName());
                textViewDocumentSigningCertificateValidFrom.setText(simpleDateFormat.format(docSigningCertificate.getNotBefore()));
                textViewDocumentSigningCertificateValidTo.setText(simpleDateFormat.format(docSigningCertificate.getNotAfter()));

            } else{
                cardViewDocumentSigningCertificate.setVisibility(View.GONE);
            }

        } else {
            cardViewDocumentSigningCertificate.setVisibility(View.GONE);
        }
    }


    private void displayAuthenticationStatus(VerificationStatus verificationStatus, FeatureStatus featureStatus){

        if(featureStatus.hasBAC()==FeatureStatus.Verdict.PRESENT){
            tableRowBac.setVisibility(View.VISIBLE);
        } else {
            tableRowBac.setVisibility(View.GONE);
        }

        if(featureStatus.hasAA()==FeatureStatus.Verdict.PRESENT){
            tableRowActive.setVisibility(View.VISIBLE);
        } else {
            tableRowActive.setVisibility(View.GONE);
        }

        if(featureStatus.hasSAC()==FeatureStatus.Verdict.PRESENT){
            tableRowPace.setVisibility(View.VISIBLE);
        } else {
            tableRowPace.setVisibility(View.GONE);
        }

        if(featureStatus.hasCA()==FeatureStatus.Verdict.PRESENT){
            tableRowChip.setVisibility(View.VISIBLE);
        } else {
            tableRowChip.setVisibility(View.GONE);
        }

        if(featureStatus.hasEAC()==FeatureStatus.Verdict.PRESENT){
            tableRowEac.setVisibility(View.VISIBLE);
        } else {
            tableRowEac.setVisibility(View.GONE);
        }

        displayVerificationStatusIcon(imageViewBAC, verificationStatus.getBAC());
        displayVerificationStatusIcon(imageViewPACE, verificationStatus.getSAC());
        displayVerificationStatusIcon(imageViewPassive, verificationStatus.getHT());
        displayVerificationStatusIcon(imageViewActive, verificationStatus.getAA());
        displayVerificationStatusIcon(imageViewDocumentSigning, verificationStatus.getDS());
        displayVerificationStatusIcon(imageViewChip, verificationStatus.getCA());
        displayVerificationStatusIcon(imageViewEac, verificationStatus.getEAC());
    }

    private void displayVerificationStatusIcon(ImageView imageView, VerificationStatus.Verdict verdict){
        if(verdict==null){
            verdict=VerificationStatus.Verdict.UNKNOWN;
        }
        int resourceIconId;
        int resourceColorId;
        switch (verdict){
            case SUCCEEDED:{
                resourceIconId=R.drawable.ic_check_circle_outline;
                resourceColorId=android.R.color.holo_green_light;
                break;
            }
            case FAILED:{
                resourceIconId=R.drawable.ic_close_circle_outline;
                resourceColorId=android.R.color.holo_red_light;
                break;
            }
            case NOT_PRESENT:{
                resourceIconId=R.drawable.ic_close_circle_outline;
                resourceColorId=android.R.color.darker_gray;
                break;
            }
            case NOT_CHECKED:{
                resourceIconId=R.drawable.ic_help_circle_outline;
                resourceColorId=android.R.color.holo_orange_light;
                break;
            }
            case UNKNOWN:
            default:{
                resourceIconId=R.drawable.ic_close_circle_outline;
                resourceColorId=android.R.color.darker_gray;
                break;
            }
        }

        imageView.setImageResource(resourceIconId);
        imageView.setColorFilter(ContextCompat.getColor(getActivity(), resourceColorId), android.graphics.PorterDuff.Mode.SRC_IN);
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
        void onImageSelected(Bitmap bitmap);
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
