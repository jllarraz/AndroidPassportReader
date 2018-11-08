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
import net.sf.scuba.smartcards.ISO7816;

import org.jmrtd.AccessDeniedException;
import org.jmrtd.BACDeniedException;
import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PACEException;
import org.jmrtd.PACEKeySpec;
import org.jmrtd.PassportService;

import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.CardSecurityFile;
import org.jmrtd.lds.ChipAuthenticationInfo;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.TerminalAuthenticationInfo;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG12File;
import org.jmrtd.lds.icao.DG14File;
import org.jmrtd.lds.icao.DG15File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.protocol.AAResult;
import org.jmrtd.protocol.BACResult;
import org.jmrtd.protocol.EACCAResult;
import org.jmrtd.protocol.EACTAResult;
import org.jmrtd.protocol.PACEResult;
import org.spongycastle.jce.provider.BouncyCastleProvider;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import example.jllarraz.com.passportreader.data.AdditionalDocumentDetails;
import example.jllarraz.com.passportreader.data.AdditionalPersonDetails;
import example.jllarraz.com.passportreader.data.Passport;
import example.jllarraz.com.passportreader.data.PersonDetails;
import example.jllarraz.com.passportreader.utils.EACCredentials;
import example.jllarraz.com.passportreader.utils.ImageUtil;
import example.jllarraz.com.passportreader.utils.MRZUtil;
import example.jllarraz.com.passportreader.utils.PassportNfcUtils;
import example.jllarraz.com.passportreader.utils.StringUtils;

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

            ps.sendSelectApplet(false);

            BACKeySpec bacKey = new BACKey(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());


            Passport passport = new Passport();


            //BAC
            BACResult bacResult = ps.doBAC(bacKey);
            passport.setBAC(true);

            //SOD FILE
            InputStream isSodFile = null;
            try{
                isSodFile= ps.getInputStream(PassportService.EF_SOD);
                SODFile sodFile = (SODFile) LDSFileUtil.getLDSFile(PassportService.EF_SOD, isSodFile);
                passport.setSodFile(sodFile);

                //Necessary for passive authentication
                Map<Integer, byte[]> dataGroupHashes = sodFile.getDataGroupHashes();
                Set<Integer> integers = dataGroupHashes.keySet();
                Iterator<Integer> iterator = integers.iterator();
                while (iterator.hasNext()){
                    Integer key = iterator.next();
                    byte[] bytes = dataGroupHashes.get(key);
                    passport.getDataGroupHashes().put(key, bytes);
                }


            }catch (Exception e){
                e.printStackTrace();
            }
            finally {
                if(isSodFile!=null){
                    isSodFile.close();
                    isSodFile = null;
                }
            }


            // Basic data
            InputStream isDG1 = null;
            try {
                isDG1 = ps.getInputStream(PassportService.EF_DG1);
                DG1File dg1 = (DG1File) LDSFileUtil.getLDSFile(PassportService.EF_DG1, isDG1);
                if (dg1 != null) {
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

                    try {
                        //Compute hash for passive authentication
                        byte[] computeHash = computeHash(passport.getSodFile(), dg1.getEncoded());
                        passport.getDataGroupComputedHashes().put(1, computeHash);
                    }catch (Exception e){
                        //Don't do anything
                    }
                }
            }finally {
                if(isDG1!=null){
                    isDG1.close();
                    isDG1 = null;
                }
            }

            //Chip Authentication
            List<EACCAResult> eaccaResults = new ArrayList<>();
            InputStream isDG14 = null;
            try {
                isDG14= ps.getInputStream(PassportService.EF_DG14);
                DG14File dg14 = (DG14File) LDSFileUtil.getLDSFile(PassportService.EF_DG14, isDG14);

                //Compute hash for passive authentication
                try{
                    byte[] computeHash = computeHash(passport.getSodFile(), dg14.getEncoded());
                    passport.getDataGroupComputedHashes().put(14, computeHash);
                }catch (Exception e){
                    //Don't do anything
                }


                ChipAuthenticationInfo chipAuthenticationInfo=null;

                List<ChipAuthenticationPublicKeyInfo> chipAuthenticationPublicKeyInfos = new ArrayList<>();
                ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = null;
                Collection<SecurityInfo> securityInfos = dg14.getSecurityInfos();
                Iterator<SecurityInfo> securityInfoIterator = securityInfos.iterator();
                while (securityInfoIterator.hasNext()){
                    SecurityInfo securityInfo = securityInfoIterator.next();
                    if(securityInfo instanceof ChipAuthenticationInfo){
                        chipAuthenticationInfo = (ChipAuthenticationInfo) securityInfo;
                    } else if(securityInfo instanceof ChipAuthenticationPublicKeyInfo){
                        chipAuthenticationPublicKeyInfos.add((ChipAuthenticationPublicKeyInfo) securityInfo);
                    }
                }

                Iterator<ChipAuthenticationPublicKeyInfo> publicKeyInfoIterator = chipAuthenticationPublicKeyInfos.iterator();
                while (publicKeyInfoIterator.hasNext()){
                    ChipAuthenticationPublicKeyInfo authenticationPublicKeyInfo = publicKeyInfoIterator.next();
                    try {
                        Log.i("EMRTD", "Chip Authentication starting");
                        EACCAResult doEACCA = ps.doEACCA(chipAuthenticationInfo.getKeyId(), chipAuthenticationInfo.getObjectIdentifier(), chipAuthenticationInfo.getProtocolOIDString(), authenticationPublicKeyInfo.getSubjectPublicKey());
                        eaccaResults.add(doEACCA);
                        Log.i("EMRTD", "Chip Authentication succeeded");
                    } catch(CardServiceException cse) {
                        cse.printStackTrace();
                        /* NOTE: Failed? Too bad, try next public key. */
                    }
                }


            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {

                    if(isDG14!=null){
                        isDG14.close();
                        isDG14 = null;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            if(eaccaResults.size()>0){
                passport.setChipAuthentication(true);
            }






            //Picture

            InputStream isDg2 = null;
            try {
                isDg2 = ps.getInputStream(PassportService.EF_DG2);
                DG2File dg2 = (DG2File) LDSFileUtil.getLDSFile(PassportService.EF_DG2, isDg2);

                //Get the picture
                try {
                    Bitmap faceImage = PassportNfcUtils.retrieveFaceImage(context, dg2);
                    passport.setFace(faceImage);
                } catch (Exception e) {
                    //Don't do anything
                    e.printStackTrace();
                }

                //Compute hash for passive authentication
                try {
                    byte[] computeHash = computeHash(passport.getSodFile(), dg2.getEncoded());
                    passport.getDataGroupComputedHashes().put(2, computeHash);
                }catch (Exception e){
                    //Don't do anything
                }
            }finally {
                if(isDg2!=null){
                    isDg2.close();
                    isDg2 = null;
                }
            }

            PACEResult paceResult = null;
            InputStream isCardAccessFile = null;
            boolean paceSucceeded = false;
            try {
                PACEKeySpec paceKeySpec = PACEKeySpec.createMRZKey(bacKey);
                isCardAccessFile = ps.getInputStream(PassportService.EF_CARD_ACCESS);

                CardAccessFile cardAccessFile = new CardAccessFile(isCardAccessFile);
                Collection<SecurityInfo> securityInfos = cardAccessFile.getSecurityInfos();
                SecurityInfo securityInfo = securityInfos.iterator().next();
                List<PACEInfo> paceInfos = new ArrayList<>();
                if (securityInfo instanceof PACEInfo) {
                    paceInfos.add((PACEInfo) securityInfo);
                }

                if (paceInfos != null && paceInfos.size() > 0) {
                    PACEInfo paceInfo = paceInfos.iterator().next();
                    paceResult = ps.doPACE(paceKeySpec, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()));
                    paceSucceeded = true;
                }else {
                    paceSucceeded = true;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            finally {
                if(isCardAccessFile!=null){
                    isCardAccessFile.close();
                    isCardAccessFile = null;
                }
            }
            passport.setPACE(paceSucceeded);


            boolean activeAuthentication =false;
            InputStream isDG15 = null;
            try {
                isDG15 = ps.getInputStream(PassportService.EF_DG15);
                DG15File dg15 = (DG15File) LDSFileUtil.getLDSFile(PassportService.EF_DG15, isDG15);
                PublicKey publicKey = dg15.getPublicKey();
                byte[] challenge = new byte[8];
                SODFile sodFile = passport.getSodFile();
                AAResult aaResult = ps.doAA(publicKey, sodFile.getDigestAlgorithm(), sodFile.getSignerInfoDigestAlgorithm(), challenge);
                //TODO Verify response
                activeAuthentication =true;
            } catch (Exception e){
                e.printStackTrace();
                activeAuthentication = false;
            }
            finally {
                if(isDG15!=null){
                    isDG15.close();
                    isDG15 = null;
                }
            }
            passport.setActiveAuthentication(activeAuthentication);

            //We need these results for the EAC
            if(paceResult!=null||eaccaResults.size()>1){
                List<EACTAResult> eactaResults = new ArrayList<>();
                InputStream isCvca=null;

                try {
                    isCvca = ps.getInputStream(PassportService.EF_CVCA);
                    CVCAFile cvca = (CVCAFile) LDSFileUtil.getLDSFile(PassportService.EF_CVCA, isCvca);
                    CVCPrincipal[] possibleCVCAReferences = new CVCPrincipal[]{ cvca.getCAReference(), cvca.getAltCAReference() };

                    //EAC
                    //First we load our keystore
                    //TODO configure real keystore
                    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    // get user password and file input stream
                    char[] password = "MY_PASSWORD".toCharArray();//Keystore password
                    try (FileInputStream fis = new FileInputStream("keyStoreName")) {
                        ks.load(fis, password);
                    }
                    List<KeyStore> keyStoreList = new ArrayList<>();
                    keyStoreList.add(ks);


                    for (CVCPrincipal caReference: possibleCVCAReferences) {
                        EACCredentials eacCredentials = PassportNfcUtils.getEACCredentials(caReference, keyStoreList);
                        if (eacCredentials == null) { continue; }

                        PrivateKey privateKey = eacCredentials.getPrivateKey();
                        Certificate[] chain = eacCredentials.getChain();
                        List<CardVerifiableCertificate> terminalCerts = new ArrayList<CardVerifiableCertificate>(chain.length);
                        for (Certificate c: chain) { terminalCerts.add((CardVerifiableCertificate)c); }

                        try{
                            if(paceResult==null) {
                                EACTAResult eactaResult = ps.doEACTA(caReference, terminalCerts, privateKey, null, eaccaResults.get(0), passport.getPersonDetails().getDocumentNumber());
                                eactaResults.add(eactaResult);
                            } else{
                                EACTAResult eactaResult = ps.doEACTA(caReference, terminalCerts, privateKey, null, eaccaResults.get(0), paceResult);
                                eactaResults.add(eactaResult);
                            }
                        } catch(CardServiceException cse) {
                            cse.printStackTrace();
                            /* NOTE: Failed? Too bad, try next public key. */
                            continue;
                        }
                        break;
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    if(isCvca!=null){
                        isCvca.close();
                        isCvca = null;
                    }
                }

                if(eactaResults.size()>0){
                    //TODO verify results
                    passport.setEAC(true);
                }


            }

            //Portrait
            //Get the picture
            InputStream isDg5 = null;
            try {
                isDg5 = ps.getInputStream(PassportService.EF_DG5);
                DG5File dg5 = (DG5File)LDSFileUtil.getLDSFile(PassportService.EF_DG5, isDg5);
                Bitmap portraitImage = PassportNfcUtils.retrievePortraitImage(context, dg5);
                passport.setPortrait(portraitImage);

                //Compute hash for passive authentication
                byte[] computeHash = computeHash(passport.getSodFile(), dg5.getEncoded());
                passport.getDataGroupComputedHashes().put(5, computeHash);
            }catch (Exception e){
              //Don't do anything
                Log.e(TAG, "Portrait image: "+e);
            }finally {
                if(isDg5!=null){
                    isDg5.close();
                    isDg5 = null;
                }
            }


            InputStream isDg11 = null;
            try {

                isDg11 = ps.getInputStream(PassportService.EF_DG11);
                DG11File dg11 = (DG11File)LDSFileUtil.getLDSFile(PassportService.EF_DG11, isDg11);

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
                try{
                    //Compute hash for passive authentication
                    byte[] computeHash = computeHash(passport.getSodFile(), dg11.getEncoded());
                    passport.getDataGroupComputedHashes().put(11, computeHash);
                }catch (Exception e){
                    //Don't do anything
                }

            }catch (Exception e){
              //Don't do anything
              Log.e(TAG, "Additional Personal Details: "+e);
            }
            finally {
                if(isDg11!=null){
                    isDg11.close();
                    isDg11 = null;
                }
            }



            //Finger prints
            //Get the pictures
            InputStream isDg3 = null;
            try {
                isDg3 = ps.getInputStream(PassportService.EF_DG3);
                DG3File dg3 = (DG3File)LDSFileUtil.getLDSFile(PassportService.EF_DG3, isDg3);
                List<Bitmap> bitmaps = PassportNfcUtils.retrieveFingerPrintImage(context, dg3);
                passport.setFingerprints(bitmaps);
                try{
                    //Compute hash for passive authentication
                    byte[] computeHash = computeHash(passport.getSodFile(), dg3.getEncoded());
                    passport.getDataGroupComputedHashes().put(3, computeHash);
                }catch (Exception e){
                    //Don't do anything
                }
            }catch (Exception e){
                //Don't do anything
                Log.e(TAG, "Fingerprint image: "+e);
            } finally {
                if(isDg3!=null){
                    isDg3.close();
                    isDg3 = null;
                }
            }


            InputStream isDg7 = null;
            try {
                isDg7 = ps.getInputStream(PassportService.EF_DG7);
                DG7File dg7 = (DG7File)LDSFileUtil.getLDSFile(PassportService.EF_DG7, isDg7);
                Bitmap bitmap = PassportNfcUtils.retrieveSignatureImage(context, dg7);
                passport.setSignature(bitmap);

                try{
                    //Compute hash for passive authentication
                    byte[] computeHash = computeHash(passport.getSodFile(), dg7.getEncoded());
                    passport.getDataGroupComputedHashes().put(7, computeHash);
                }catch (Exception e){
                    //Don't do anything
                }
            }catch (Exception e){
                //Don't do anything
                Log.e(TAG, "Signature image: "+e);
            } finally {
                if(isDg7!=null){
                    isDg7.close();
                    isDg7 = null;
                }
            }

            InputStream isDg12 = null;
            try {
                isDg12 = ps.getInputStream(PassportService.EF_DG12);
                DG12File dg12 = (DG12File) LDSFileUtil.getLDSFile(PassportService.EF_DG12, isDg12);
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
                try{
                    //Compute hash for passive authentication
                    byte[] computeHash = computeHash(passport.getSodFile(), dg12.getEncoded());
                    passport.getDataGroupComputedHashes().put(12, computeHash);
                }catch (Exception e){
                    //Don't do anything
                }

            }catch (Exception e){
                //Don't do anything
                Log.e(TAG, "Additional document details: "+e);
            }
            finally {
                if(isDg12!=null){
                    isDg12.close();
                    isDg12 = null;
                }
            }

            boolean passiveAuthentication = passiveAuthentication(passport.getDataGroupHashes(), passport.getDataGroupComputedHashes());
            passport.setPassiveAuthentication(passiveAuthentication);

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

  private byte[] computeHash(SODFile sodFile, byte[] data) throws NoSuchAlgorithmException {
      String digestAlgorithm = passport.getSodFile().getDigestAlgorithm();
      MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
      byte[] digest = md.digest(data);
      return digest;
  }

  private static boolean passiveAuthentication(Map<Integer, byte[]> groupHash, Map<Integer, byte[]> groupComputed){
      Set<Integer> integerSet = groupHash.keySet();
      Iterator<Integer> iterator = integerSet.iterator();
      while (iterator.hasNext()){
          Integer key = iterator.next();
          if(groupComputed.containsKey(key)){
              String hashStored=StringUtils.bytesToHex(groupHash.get(key));
              String hashComputed=StringUtils.bytesToHex(groupComputed.get(key));
              if(!hashStored.equalsIgnoreCase(hashComputed)){
                  return false;
              }
          }
      }
      return true;
  }
}
