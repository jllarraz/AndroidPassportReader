/*
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.jllarraz.com.passportreader.asynctask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.util.Log;

import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;

import org.jmrtd.AccessDeniedException;
import org.jmrtd.BACDeniedException;
import org.jmrtd.MRTDTrustStore;
import org.jmrtd.PACEException;

import org.jmrtd.PassportService;

import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG12File;

import org.jmrtd.lds.icao.MRZInfo;
import java.security.Security;
import java.util.Collections;

import java.util.List;

import example.jllarraz.com.passportreader.data.AdditionalDocumentDetails;
import example.jllarraz.com.passportreader.data.AdditionalPersonDetails;
import org.jmrtd.FeatureStatus;
import example.jllarraz.com.passportreader.data.Passport;
import example.jllarraz.com.passportreader.data.PersonDetails;
import org.jmrtd.VerificationStatus;

import example.jllarraz.com.passportreader.utils.PassportNFC;
import example.jllarraz.com.passportreader.utils.PassportNfcUtils;


public final class NfcPassportAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = NfcPassportAsyncTask.class.getSimpleName();

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }


    private Context context;
    private Tag tag;
    private MRZInfo mrzInfo;
    private NfcPassportAsyncTaskListener nfcPassportAsyncTaskListener;

    private Exception cardServiceException;
    private Passport passport;

    private final static List EMPTY_TRIED_BAC_ENTRY_LIST = Collections.emptyList();
    private final static List EMPTY_CERTIFICATE_CHAIN = Collections.emptyList();


    public NfcPassportAsyncTask(Context context, Tag imageProcessor, MRZInfo mrzInfo, NfcPassportAsyncTaskListener nfcPassportAsyncTaskListener) {
        this.context = context;
        this.tag = imageProcessor;
        this.mrzInfo = mrzInfo;
        this.nfcPassportAsyncTaskListener = nfcPassportAsyncTaskListener;
    }

    @Override
    protected Boolean doInBackground(Void... arg0) {
        return handle(tag);
    }

    @Override
    protected void onPostExecute(Boolean result) {
    super.onPostExecute(result);
        if(result){
            onPassportRead(passport);
        }
        else {
            onCardException(cardServiceException);
        }
    }


    protected boolean handle(Tag tag){
        PassportService ps = null;

        try {
            IsoDep nfc = IsoDep.get(tag);
            CardService cs = CardService.getInstance(nfc);

            ps = new PassportService(cs, 256, 224, false, true);
            ps.open();

            PassportNFC passportNFC =  new PassportNFC(ps, new MRTDTrustStore(), mrzInfo);
            VerificationStatus verifySecurity = passportNFC.verifySecurity();
            FeatureStatus features = passportNFC.getFeatures();

            passport = new Passport();

            passport.setFeatureStatus(passportNFC.getFeatures());
            passport.setVerificationStatus(passportNFC.getVerificationStatus());


            passport.setSodFile(passportNFC.getSodFile());


            //Basic Information
            if (passportNFC.getDg1File() != null) {
                MRZInfo mrzInfo = passportNFC.getDg1File().getMRZInfo();
                PersonDetails personDetails = new PersonDetails();
                personDetails.setDateOfBirth(mrzInfo.getDateOfBirth());
                personDetails.setDateOfExpiry(mrzInfo.getDateOfExpiry());
                personDetails.setDocumentCode(mrzInfo.getDocumentCode());
                personDetails.setDocumentNumber(mrzInfo.getDocumentNumber());
                personDetails.setOptionalData1(mrzInfo.getOptionalData1());
                personDetails.setOptionalData2(mrzInfo.getOptionalData2());
                personDetails.setIssuingState(mrzInfo.getIssuingState());
                personDetails.setPrimaryIdentifier(mrzInfo.getPrimaryIdentifier());
                personDetails.setSecondaryIdentifier(mrzInfo.getSecondaryIdentifier());
                personDetails.setNationality(mrzInfo.getNationality());
                personDetails.setGender(mrzInfo.getGender());
                passport.setPersonDetails(personDetails);
            }

            //Picture
            if (passportNFC.getDg2File() != null) {
                //Get the picture
                try {
                    Bitmap faceImage = PassportNfcUtils.retrieveFaceImage(context, passportNFC.getDg2File());
                    passport.setFace(faceImage);
                } catch (Exception e) {
                    //Don't do anything
                    e.printStackTrace();
                }
            }


            //Portrait
            //Get the picture
            if (passportNFC.getDg5File() != null) {
                //Get the picture
                try {
                    Bitmap faceImage = PassportNfcUtils.retrievePortraitImage(context, passportNFC.getDg5File());
                    passport.setPortrait(faceImage);
                } catch (Exception e) {
                    //Don't do anything
                    e.printStackTrace();
                }
            }


            DG11File dg11 = passportNFC.getDg11File();
            if(dg11 !=null){

                AdditionalPersonDetails additionalPersonDetails = new AdditionalPersonDetails();
                additionalPersonDetails.setCustodyInformation(dg11.getCustodyInformation());
                additionalPersonDetails.setFullDateOfBirth(dg11.getFullDateOfBirth());
                additionalPersonDetails.setNameOfHolder(dg11.getNameOfHolder());
                additionalPersonDetails.setOtherNames(dg11.getOtherNames());
                additionalPersonDetails.setOtherNames(dg11.getOtherNames());
                additionalPersonDetails.setOtherValidTDNumbers(dg11.getOtherValidTDNumbers());
                additionalPersonDetails.setPermanentAddress(dg11.getPermanentAddress());
                additionalPersonDetails.setPersonalNumber(dg11.getPersonalNumber());
                additionalPersonDetails.setPersonalSummary(dg11.getPersonalSummary());
                additionalPersonDetails.setPlaceOfBirth(dg11.getPlaceOfBirth());
                additionalPersonDetails.setProfession(dg11.getProfession());
                additionalPersonDetails.setProofOfCitizenship(dg11.getProofOfCitizenship());
                additionalPersonDetails.setTag(dg11.getTag());
                additionalPersonDetails.setTagPresenceList(dg11.getTagPresenceList());
                additionalPersonDetails.setTelephone(dg11.getTelephone());
                additionalPersonDetails.setTitle(dg11.getTitle());

                passport.setAdditionalPersonDetails(additionalPersonDetails);
            }


            //Finger prints
            //Get the pictures
            if (passportNFC.getDg3File() != null) {
                //Get the picture
                try {
                    List<Bitmap> bitmaps = PassportNfcUtils.retrieveFingerPrintImage(context, passportNFC.getDg3File());
                    passport.setFingerprints(bitmaps);
                } catch (Exception e) {
                    //Don't do anything
                    e.printStackTrace();
                }
            }


            //Signature
            //Get the pictures
            if (passportNFC.getDg7File() != null) {
                //Get the picture
                try {
                    Bitmap bitmap = PassportNfcUtils.retrieveSignatureImage(context, passportNFC.getDg7File());
                    passport.setSignature(bitmap);
                } catch (Exception e) {
                    //Don't do anything
                    e.printStackTrace();
                }
            }

            //Additional Document Details

            DG12File dg12 = passportNFC.getDg12File();
            if (dg12 != null) {
                AdditionalDocumentDetails additionalDocumentDetails = new AdditionalDocumentDetails();
                additionalDocumentDetails.setDateAndTimeOfPersonalization(dg12.getDateAndTimeOfPersonalization());
                additionalDocumentDetails.setDateOfIssue(dg12.getDateOfIssue());
                additionalDocumentDetails.setEndorsementsAndObservations(dg12.getEndorsementsAndObservations());
                try {
                    byte[] imageOfFront = dg12.getImageOfFront();
                    Bitmap bitmapImageOfFront = BitmapFactory.decodeByteArray(imageOfFront, 0, imageOfFront.length);
                    additionalDocumentDetails.setImageOfFront(bitmapImageOfFront);
                } catch (Exception e) {
                    Log.e(TAG, "Additional document image front: " + e);
                }
                try {
                    byte[] imageOfRear = dg12.getImageOfRear();
                    Bitmap bitmapImageOfRear = BitmapFactory.decodeByteArray(imageOfRear, 0, imageOfRear.length);
                    additionalDocumentDetails.setImageOfRear(bitmapImageOfRear);
                } catch (Exception e) {
                    Log.e(TAG, "Additional document image rear: " + e);
                }
                additionalDocumentDetails.setIssuingAuthority(dg12.getIssuingAuthority());
                additionalDocumentDetails.setNamesOfOtherPersons(dg12.getNamesOfOtherPersons());
                additionalDocumentDetails.setPersonalizationSystemSerialNumber(dg12.getPersonalizationSystemSerialNumber());
                additionalDocumentDetails.setTaxOrExitRequirements(dg12.getTaxOrExitRequirements());

                passport.setAdditionalDocumentDetails(additionalDocumentDetails);
            }

            this.passport = passport;
            //TODO EAC
    } catch (Exception e) {
        cardServiceException = e;
        return false;
    } finally {
        try {
            if (ps != null){
                ps.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    return true;
  }

  protected void onCardException(Exception exception){
    if(nfcPassportAsyncTaskListener!=null){
        if(cardServiceException instanceof AccessDeniedException){
            nfcPassportAsyncTaskListener.onAccessDeniedException((AccessDeniedException) exception);
        } else if(cardServiceException instanceof BACDeniedException){
            nfcPassportAsyncTaskListener.onBACDeniedException((BACDeniedException) exception);
        } else if(cardServiceException instanceof PACEException){
            nfcPassportAsyncTaskListener.onPACEException((PACEException) exception);
        } else if(cardServiceException instanceof CardServiceException){
            nfcPassportAsyncTaskListener.onCardException((CardServiceException) exception);
        } else {
            nfcPassportAsyncTaskListener.onGeneralException(exception);
        }
    }
  }

  protected void onPassportRead(final Passport passport){
    if(nfcPassportAsyncTaskListener!=null){
      nfcPassportAsyncTaskListener.onPassportRead(passport);
    }
  }

  public interface NfcPassportAsyncTaskListener{
    void onPassportRead(Passport passport);
    void onAccessDeniedException(AccessDeniedException exception);
    void onBACDeniedException(BACDeniedException exception);
    void onPACEException(PACEException exception);
    void onCardException(CardServiceException exception);
    void onGeneralException(Exception exception);
  }
}
