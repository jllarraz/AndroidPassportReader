package example.jllarraz.com.passportreader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import org.jmrtd.lds.iso19794.FingerImageInfo;
import org.jmrtd.lds.iso19794.FingerInfo;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

public class PassportNfcUtils {

    private static final String TAG = PassportNfcUtils.class.getSimpleName();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    private static final boolean IS_PKIX_REVOCATION_CHECING_ENABLED = false;



    public static Bitmap retrieveFaceImage(Context context, DG2File dg2File) throws IOException {
        List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
        List<FaceInfo> faceInfos = dg2File.getFaceInfos();
        for (FaceInfo faceInfo : faceInfos) {
            allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
        }

        if (!allFaceImageInfos.isEmpty()) {
            FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();

            int imageLength = faceImageInfo.getImageLength();
            DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
            byte[] buffer = new byte[imageLength];
            dataInputStream.readFully(buffer, 0, imageLength);
            InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);

           // return ImageUtil.decodeImage(context, faceImageInfo.getMimeType(), inputStream);
            return ImageUtil.decodeImage(inputStream, imageLength, faceImageInfo.getMimeType());

        }
        throw new IOException("Unable to decodeImage Image");
    }

    public static Bitmap retrievePortraitImage(Context context, DG5File dg2File) throws IOException {
        List<DisplayedImageInfo> faceInfos = dg2File.getImages();
        if (!faceInfos.isEmpty()) {
            DisplayedImageInfo faceImageInfo = faceInfos.iterator().next();

            int imageLength = faceImageInfo.getImageLength();
            DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
            byte[] buffer = new byte[imageLength];
            dataInputStream.readFully(buffer, 0, imageLength);
            InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);

            return ImageUtil.decodeImage(inputStream, imageLength, faceImageInfo.getMimeType());

        }
        throw new IOException("Unable to decodeImage Image");
    }

    public static Bitmap retrieveSignatureImage(Context context, DG7File dg7File) throws IOException {
        List<DisplayedImageInfo> displayedImageInfos = dg7File.getImages();
        if (!displayedImageInfos.isEmpty()) {
            DisplayedImageInfo faceImageInfo = displayedImageInfos.iterator().next();

            int imageLength = faceImageInfo.getImageLength();
            DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
            byte[] buffer = new byte[imageLength];
            dataInputStream.readFully(buffer, 0, imageLength);
            InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);

            return ImageUtil.decodeImage(inputStream, imageLength, faceImageInfo.getMimeType());

        }
        throw new IOException("Unable to decodeImage Image");
    }

    public static List<Bitmap> retrieveFingerPrintImage(Context context, DG3File dg3File) throws IOException {
        List<FingerImageInfo> allFingerImageInfos = new ArrayList<>();
        List<FingerInfo> fingerInfos = dg3File.getFingerInfos();

        List<Bitmap> fingerprintsImage=new ArrayList<>();
        for (FingerInfo fingerInfo : fingerInfos) {
            allFingerImageInfos.addAll(fingerInfo.getFingerImageInfos());
        }

        Iterator<FingerImageInfo> iterator = allFingerImageInfos.iterator();
        while (iterator.hasNext()){
            FingerImageInfo fingerImageInfo = iterator.next();
            int imageLength = fingerImageInfo.getImageLength();
            DataInputStream dataInputStream = new DataInputStream(fingerImageInfo.getImageInputStream());
            byte[] buffer = new byte[imageLength];
            dataInputStream.readFully(buffer, 0, imageLength);
            InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);

            Bitmap bitmap = ImageUtil.decodeImage(inputStream, imageLength, fingerImageInfo.getMimeType());
            fingerprintsImage.add(bitmap);
        }

        if (fingerprintsImage.isEmpty()) {
            throw new IOException("Unable to decodeImage Finger print Image");
        }
        return fingerprintsImage;

    }





    public static EACCredentials getEACCredentials(CVCPrincipal caReference, List<KeyStore> cvcaStores) throws GeneralSecurityException {
        for (KeyStore cvcaStore: cvcaStores) {
            EACCredentials eacCredentials = getEACCredentials(caReference, cvcaStore);
            if (eacCredentials != null) { return eacCredentials; }
        }
        return null;
    }

    /**
     * Searches the key store for a relevant terminal key and associated certificate chain.
     *
     * @param caReference
     * @param cvcaStore should contain a single key with certificate chain
     * @return
     * @throws GeneralSecurityException
     */
    private static EACCredentials getEACCredentials(CVCPrincipal caReference, KeyStore cvcaStore) throws GeneralSecurityException {
        if (caReference == null) { throw new IllegalArgumentException("CA reference cannot be null"); }

        PrivateKey privateKey = null;
        Certificate[] chain = null;

        List<String> aliases = Collections.list(cvcaStore.aliases());
        for (String alias: aliases) {
            if (cvcaStore.isKeyEntry(alias)) {
                Key key = cvcaStore.getKey(alias, "".toCharArray());
                if (key instanceof PrivateKey) {
                    privateKey = (PrivateKey)key;
                } else {
                    Log.w(TAG, "skipping non-private key " + alias);
                    continue;
                }
                chain = cvcaStore.getCertificateChain(alias);
                return new EACCredentials(privateKey, chain);
            } else if (cvcaStore.isCertificateEntry(alias)) {
                CardVerifiableCertificate certificate = (CardVerifiableCertificate)cvcaStore.getCertificate(alias);
                CVCPrincipal authRef = certificate.getAuthorityReference();
                CVCPrincipal holderRef = certificate.getHolderReference();
                if (!caReference.equals(authRef)) { continue; }
                /* See if we have a private key for that certificate. */
                privateKey = (PrivateKey)cvcaStore.getKey(holderRef.getName(), "".toCharArray());
                chain = cvcaStore.getCertificateChain(holderRef.getName());
                if (privateKey == null) { continue; }
                Log.i(TAG, "found a key, privateKey = " + privateKey);
                return new EACCredentials(privateKey, chain);
            }
            if (privateKey == null || chain == null) {
                Log.e(TAG, "null chain or key for entry " + alias + ": chain = " + Arrays.toString(chain) + ", privateKey = " + privateKey);
                continue;
            }
        }
        return null;
    }

    /**
     * Builds a certificate chain to an anchor using the PKIX algorithm.
     *
     * @param docSigningCertificate the start certificate
     * @param sodIssuer the issuer of the start certificate (ignored unless <code>docSigningCertificate</code> is <code>null</code>)
     * @param sodSerialNumber the serial number of the start certificate (ignored unless <code>docSigningCertificate</code> is <code>null</code>)
     *
     * @return the certificate chain
     */
    private static List<Certificate> getCertificateChain(X509Certificate docSigningCertificate,
                                                         final X500Principal sodIssuer, final BigInteger sodSerialNumber,
                                                         List<CertStore> cscaStores, Set<TrustAnchor> cscaTrustAnchors) {
        List<Certificate> chain = new ArrayList<Certificate>();
        X509CertSelector selector = new X509CertSelector();
        try {

            if (docSigningCertificate != null) {
                selector.setCertificate(docSigningCertificate);
            } else {
                selector.setIssuer(sodIssuer);
                selector.setSerialNumber(sodSerialNumber);
            }

            CertStoreParameters docStoreParams = new CollectionCertStoreParameters(Collections.singleton((Certificate)docSigningCertificate));
            CertStore docStore = CertStore.getInstance("Collection", docStoreParams);

            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "SC");//Spungy castle
            PKIXBuilderParameters buildParams = new PKIXBuilderParameters(cscaTrustAnchors, selector);
            buildParams.addCertStore(docStore);
            for (CertStore trustStore: cscaStores) {
                buildParams.addCertStore(trustStore);
            }
            buildParams.setRevocationEnabled(IS_PKIX_REVOCATION_CHECING_ENABLED); /* NOTE: set to false for checking disabled. */

            PKIXCertPathBuilderResult result = null;

            try {
                result = (PKIXCertPathBuilderResult)builder.build(buildParams);
            } catch (CertPathBuilderException cpbe) {
                /* NOTE: ignore, result remain null */
            }
            if (result != null) {
                CertPath pkixCertPath = result.getCertPath();
                if (pkixCertPath != null) {
                    chain.addAll(pkixCertPath.getCertificates());
                }
            }
            if (docSigningCertificate != null && !chain.contains(docSigningCertificate)) {
                /* NOTE: if doc signing certificate not in list, we add it ourselves. */
                Log.w(TAG, "Adding doc signing certificate after PKIXBuilder finished");
                chain.add(0, docSigningCertificate);
            }
            if (result != null) {
                Certificate trustAnchorCertificate = result.getTrustAnchor().getTrustedCert();
                if (trustAnchorCertificate != null && !chain.contains(trustAnchorCertificate)) {
                    /* NOTE: if trust anchor not in list, we add it ourselves. */
                    Log.w(TAG, "Adding trust anchor certificate after PKIXBuilder finished");
                    chain.add(trustAnchorCertificate);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Building a chain failed (" + e.getMessage() + ").");
        }
        return chain;
    }

}
