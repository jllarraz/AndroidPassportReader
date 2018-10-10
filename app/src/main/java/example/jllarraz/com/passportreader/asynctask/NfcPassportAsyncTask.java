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

import net.sf.scuba.data.Gender;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;

import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;

import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG12File;
import org.jmrtd.lds.icao.DG14File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.protocol.CAResult;


import java.io.FileInputStream;
import java.io.InputStream;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManagerFactory;

import example.jllarraz.com.passportreader.data.AdditionalDocumentDetails;
import example.jllarraz.com.passportreader.data.AdditionalPersonDetails;
import example.jllarraz.com.passportreader.data.Passport;
import example.jllarraz.com.passportreader.data.PersonDetails;
import example.jllarraz.com.passportreader.utils.ImageUtil;
import example.jllarraz.com.passportreader.utils.MRZUtil;
import example.jllarraz.com.passportreader.utils.PassportNfcUtils;

public final class NfcPassportAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = NfcPassportAsyncTask.class.getSimpleName();

    private Context context;
    private Tag tag;
    private MRZInfo mrzInfo;
    private NfcPassportAsyncTaskListener nfcPassportAsyncTaskListener;

    private CardServiceException cardServiceException;
    private Passport passport;


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
            ps = new PassportService(cs);
            ps.open();

            ps.sendSelectApplet(false);
            BACKeySpec bacKey = new BACKeySpec() {
                @Override
                public String getDocumentNumber() {
                  return mrzInfo.getDocumentNumber();
                }

                @Override
                public String getDateOfBirth() {
                  return mrzInfo.getDateOfBirth();
                }

                @Override
                public String getDateOfExpiry() {
                  return mrzInfo.getDateOfExpiry();
                }
                };

            ps.doBAC(bacKey);

            InputStream is = null;
            InputStream isPicture = null;
            InputStream isPortrait = null;
            InputStream isFingerprint = null;
            InputStream isSignature = null;
            InputStream isPassportExtraDetails = null;
            InputStream isAdditionalPersonalDetails = null;
            try {

                Passport passport = new Passport();
                // Basic data
                is = ps.getInputStream(PassportService.EF_DG1);
                DG1File dg1 = (DG1File) LDSFileUtil.getLDSFile(PassportService.EF_DG1, is);
                if(dg1!=null){

                    MRZInfo mrzInfo = dg1.getMRZInfo();
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
                isPicture = ps.getInputStream(PassportService.EF_DG2);
                DG2File dg2 = (DG2File)LDSFileUtil.getLDSFile(PassportService.EF_DG2, isPicture);

                //Get the picture
                try {
                    Bitmap faceImage = PassportNfcUtils.retrieveFaceImage(context, dg2);
                    passport.setFace(faceImage);
                }catch (Exception e){
                  //Don't do anything
                  e.printStackTrace();
                }


                // Chip Authentication
                List<CAResult> caResults = PassportNfcUtils.doChipAuthentication(ps);
                if(caResults.size()>0){
                    Log.d(TAG, "Chip authentication success ");
                    passport.setChipAuthentication(true);
                }


                //Portrait
                //Get the picture
                try {
                    isPortrait = ps.getInputStream(PassportService.EF_DG5);
                    DG5File dg5 = (DG5File)LDSFileUtil.getLDSFile(PassportService.EF_DG5, isPortrait);
                    Bitmap portraitImage = PassportNfcUtils.retrievePortraitImage(context, dg5);
                    passport.setPortrait(portraitImage);
                }catch (Exception e){
                  //Don't do anything
                    Log.e(TAG, "Portrait image: "+e);
                }

                try {

                    isAdditionalPersonalDetails = ps.getInputStream(PassportService.EF_DG11);
                    DG11File dg11 = (DG11File)LDSFileUtil.getLDSFile(PassportService.EF_DG11, isAdditionalPersonalDetails);

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

                }catch (Exception e){
                  //Don't do anything
                  Log.e(TAG, "Additional Personal Details: "+e);
                }

                /*
                //EAC
                //First we load our keystore
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                // get user password and file input stream
                char[] password = "MY_PASSWORD".toCharArray();//Keystore password
                try (FileInputStream fis = new FileInputStream("keyStoreName")) {
                  ks.load(fis, password);
                }
                List<KeyStore> keyStoreList = new ArrayList<>();
                keyStoreList.add(ks);

                //WE try to do EAC with the certificates in our Keystore
                PassportNfcUtils.doEac(ps, dg1.getMRZInfo().getDocumentNumber(), keyStoreList);
                */

                //Finger prints
                //Get the pictures
                try {
                    isFingerprint = ps.getInputStream(PassportService.EF_DG3);
                    DG3File dg3 = (DG3File)LDSFileUtil.getLDSFile(PassportService.EF_DG3, isFingerprint);
                    List<Bitmap> bitmaps = PassportNfcUtils.retrieveFingerPrintImage(context, dg3);
                    passport.setFingerprints(bitmaps);
                }catch (Exception e){
                    //Don't do anything
                    Log.e(TAG, "Fingerprint image: "+e);
                }


                try {
                    isSignature = ps.getInputStream(PassportService.EF_DG7);
                    DG7File dg7 = (DG7File)LDSFileUtil.getLDSFile(PassportService.EF_DG7, isSignature);
                    Bitmap bitmap = PassportNfcUtils.retrieveSignatureImage(context, dg7);
                    passport.setSignature(bitmap);
                }catch (Exception e){
                    //Don't do anything
                    Log.e(TAG, "Signature image: "+e);
                }

                try {
                    isPassportExtraDetails = ps.getInputStream(PassportService.EF_DG12);
                    DG12File dg12 = (DG12File)LDSFileUtil.getLDSFile(PassportService.EF_DG12, isPassportExtraDetails);
                    AdditionalDocumentDetails additionalDocumentDetails = new AdditionalDocumentDetails();
                    additionalDocumentDetails.setDateAndTimeOfPersonalization(dg12.getDateAndTimeOfPersonalization());
                    additionalDocumentDetails.setDateOfIssue(dg12.getDateOfIssue());
                    additionalDocumentDetails.setEndorsementsAndObservations(dg12.getEndorsementsAndObservations());
                    try {
                        byte[] imageOfFront = dg12.getImageOfFront();
                        Bitmap bitmapImageOfFront = BitmapFactory.decodeByteArray(imageOfFront, 0, imageOfFront.length);
                        additionalDocumentDetails.setImageOfFront(bitmapImageOfFront);
                    }catch (Exception e){
                        Log.e(TAG, "Additional document image front: "+e);
                    }
                    try {
                        byte[] imageOfRear = dg12.getImageOfRear();
                        Bitmap bitmapImageOfRear = BitmapFactory.decodeByteArray(imageOfRear, 0, imageOfRear.length);
                        additionalDocumentDetails.setImageOfRear(bitmapImageOfRear);
                    }catch (Exception e){
                        Log.e(TAG, "Additional document image rear: "+e);
                    }
                    additionalDocumentDetails.setIssuingAuthority(dg12.getIssuingAuthority());
                    additionalDocumentDetails.setNamesOfOtherPersons(dg12.getNamesOfOtherPersons());
                    additionalDocumentDetails.setPersonalizationSystemSerialNumber(dg12.getPersonalizationSystemSerialNumber());
                    additionalDocumentDetails.setTaxOrExitRequirements(dg12.getTaxOrExitRequirements());

                    passport.setAdditionalDocumentDetails(additionalDocumentDetails);

                }catch (Exception e){
                    //Don't do anything
                    Log.e(TAG, "Additional document details: "+e);
                }

                this.passport = passport;
            //TODO EAC
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            try {
                if(is!=null) {
                    is.close();
                }
                if(isPicture!=null) {
                    isPicture.close();
                }
                if(isPortrait!=null) {
                  isPortrait.close();
                }

                if(isFingerprint!=null){
                    isFingerprint.close();
                }

                if(isSignature!=null){
                    isSignature.close();
                }

                if(isAdditionalPersonalDetails!=null) {
                    isAdditionalPersonalDetails.close();
                }

                if(isPassportExtraDetails!=null){
                    isPassportExtraDetails.close();
                }
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
    } catch (CardServiceException e) {
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

  protected void onCardException(CardServiceException cardException){
    if(nfcPassportAsyncTaskListener!=null){
        nfcPassportAsyncTaskListener.onCardException(cardException);
    }
  }

  protected void onPassportRead(final Passport passport){
    if(nfcPassportAsyncTaskListener!=null){
      nfcPassportAsyncTaskListener.onPassportRead(passport);
    }
  }

  public interface NfcPassportAsyncTaskListener{
    void onPassportRead(Passport passport);
    void onCardException(CardServiceException cardException);
  }
}
