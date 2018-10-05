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
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;

import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;

import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.MRZInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import example.jllarraz.com.passportreader.utils.ImageUtil;
import example.jllarraz.com.passportreader.utils.PassportNfcUtils;

public final class NfcPassportAsyncTask extends AsyncTask<Void, Void, Boolean> {

  private Context context;
  private Tag tag;
  private MRZInfo mrzInfo;
  private NfcPassportAsyncTaskListener nfcPassportAsyncTaskListener;

  private CardServiceException cardServiceException;
  private MRZInfo mrzInfoResult;
  private Bitmap faceImage;


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
        onPassportRead(mrzInfoResult, faceImage);
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
      InputStream is14 = null;
      InputStream isCvca = null;
      InputStream isPicture = null;
      try {
        // Basic data
        is = ps.getInputStream(PassportService.EF_DG1);
        DG1File dg1 = (DG1File) LDSFileUtil.getLDSFile(PassportService.EF_DG1, is);

        //Picture
        isPicture = ps.getInputStream(PassportService.EF_DG2);
        DG2File dg2 = (DG2File)LDSFileUtil.getLDSFile(PassportService.EF_DG2, isPicture);

        //Get the picture
        Bitmap faceImage = null;
        try {
          faceImage = PassportNfcUtils.retrieveFaceImage(context, dg2);
        }catch (Exception e){
          //Don't do anything
          e.printStackTrace();
        }

        //We don't want any authentication for now, just public data
        // Chip Authentication
                /*
                is14 = ps.getInputStream(PassportService.EF_DG14);
                DG14File dg14 = (DG14File) LDSFileUtil.getLDSFile(PassportService.EF_DG14, is14);
                Map<BigInteger, PublicKey> keyInfo = dg14.getChipAuthenticationPublicKeyInfos();
                Map.Entry<BigInteger, PublicKey> entry = keyInfo.entrySet().iterator().next();
                Log.i("EMRTD", "Chip Authentication starting");
                ChipAuthenticationResult caResult = doCA(ps, entry.getKey(), entry.getValue());
                Log.i("EMRTD", "Chip authentnication succeeded");

                // CVCA
                isCvca = ps.getInputStream(PassportService.EF_CVCA);
                CVCAFile cvca = (CVCAFile) LDSFileUtil.getLDSFile(PassportService.EF_CVCA, isCvca);
                */

                mrzInfoResult = dg1.getMRZInfo();
                this.faceImage = faceImage;
        //TODO EAC
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          is.close();
          // is14.close();
          //isCvca.close();
          isPicture.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (CardServiceException e) {
      cardServiceException = e;
      return false;
    } finally {
      try {
        ps.close();
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

  protected void onPassportRead(final MRZInfo personInfo, final Bitmap faceImage){
    if(nfcPassportAsyncTaskListener!=null){
      nfcPassportAsyncTaskListener.onPassportRead(personInfo, faceImage);
    }
  }

  public interface NfcPassportAsyncTaskListener{
    void onPassportRead(MRZInfo personInfo, Bitmap faceImage);
    void onCardException(CardServiceException cardException);
  }
}
