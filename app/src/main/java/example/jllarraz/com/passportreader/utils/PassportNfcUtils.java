package example.jllarraz.com.passportreader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.tlv.TLVOutputStream;


import org.jmrtd.PassportService;

import org.jmrtd.Util;
import org.jmrtd.cert.CVCAuthorizationTemplate;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.icao.DG14File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import org.jmrtd.lds.iso19794.FingerImageInfo;
import org.jmrtd.lds.iso19794.FingerInfo;
/*import org.jmrtd.protocol.CAResult;
import org.jmrtd.protocol.DESedeSecureMessagingWrapper;
import org.jmrtd.protocol.SecureMessagingWrapper;
import org.jmrtd.protocol.TAResult;*/
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
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
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.security.auth.x500.X500Principal;

public class PassportNfcUtils {

    private static final String TAG = PassportNfcUtils.class.getSimpleName();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final int TAG_CVCERTIFICATE_SIGNATURE = 0x5F37;

    private static final boolean IS_PKIX_REVOCATION_CHECING_ENABLED = false;

    /**
     * Copy pasted, because original uses explicit cast to BouncyCastle key implementation, whereas we have a spongycastle one
     */

    /**
     * Perform CA (Chip Authentication) part of EAC (version 1). For details see TR-03110
     * ver. 1.11. In short, we authenticate the chip with (EC)DH key agreement
     * protocol and create new secure messaging keys.
     *
     * @param keyId passport's public key id (stored in DG14), -1 if none
     * @param publicKey passport's public key (stored in DG14)
     *
     * @return the chip authentication result
     *
     * @throws CardServiceException if CA failed or some error occurred
     */
    /*
    public static CAResult doCA(PassportService ps, BigInteger keyId, PublicKey publicKey) throws CardServiceException {
        if (publicKey == null) { throw new IllegalArgumentException("Public key is null"); }
        try {
            String agreementAlg = Util.inferKeyAgreementAlgorithm(publicKey);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(agreementAlg);
            AlgorithmParameterSpec params = null;
            if ("DH".equals(agreementAlg)) {
                DHPublicKey dhPublicKey = (DHPublicKey)publicKey;
                params = dhPublicKey.getParams();
            } else if ("ECDH".equals(agreementAlg)) {
                ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
                params = ecPublicKey.getParams();
            } else {
                throw new IllegalStateException("Unsupported algorithm \"" + agreementAlg + "\"");
            }
            keyPairGenerator.initialize(params);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            KeyAgreement agreement = KeyAgreement.getInstance(agreementAlg);
            agreement.init(keyPair.getPrivate());
            agreement.doPhase(publicKey, true);

            byte[] secret = agreement.generateSecret();

            // TODO: this SHA1ing may have to be removed?
            // TODO: this hashing is needed for our Java Card passport applet implementation
            // byte[] secret = md.digest(secret);

            byte[] keyData = null;
            byte[] idData = null;
            byte[] keyHash = new byte[0];
            if ("DH".equals(agreementAlg)) {
                DHPublicKey dhPublicKey = (DHPublicKey)keyPair.getPublic();
                keyData = dhPublicKey.getY().toByteArray();
                // TODO: this is probably wrong, what should be hashed?
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md = MessageDigest.getInstance("SHA1");
                keyHash = md.digest(keyData);
            } else if ("ECDH".equals(agreementAlg)) {
                org.spongycastle.jce.interfaces.ECPublicKey ecPublicKey = (org.spongycastle.jce.interfaces.ECPublicKey)keyPair.getPublic();
                keyData = ecPublicKey.getQ().getEncoded();
                byte[] t = Util.i2os(ecPublicKey.getQ().getX().toBigInteger());
                keyHash = Util.alignKeyDataToSize(t, ecPublicKey.getParameters().getCurve().getFieldSize() / 8);
            }
            keyData = Util.wrapDO((byte)0x91, keyData);
            if (keyId.compareTo(BigInteger.ZERO) >= 0) {
                byte[] keyIdBytes = keyId.toByteArray();
                idData = Util.wrapDO((byte)0x84, keyIdBytes);
            }
            ps.sendMSEKAT(ps.getWrapper(), keyData, idData);

            *//* Start secure messaging. *//*
            SecretKey ksEnc = Util.deriveKey(secret, Util.ENC_MODE);
            SecretKey ksMac = Util.deriveKey(secret, Util.MAC_MODE);
            ps.setWrapper(new DESedeSecureMessagingWrapper(ksEnc, ksMac, 0L));
            //wrapper = new DESedeSecureMessagingWrapper(ksEnc, ksMac, 0L); // FIXME: can be AESSecureMessagingWrapper for EAC 2. -- MO


            //Not sure that this should be here
            Field fld = PassportService.class.getDeclaredField("state");
            fld.setAccessible(true);
            fld.set(ps, Enum.valueOf((Class<Enum>) fld.getType(), "CA_EXECUTED_STATE")) ;


            return new CAResult(keyId, publicKey, keyHash, keyPair.getPublic(), keyPair.getPrivate(), (SecureMessagingWrapper) ps.getWrapper());
        } catch (Exception e) {
            throw new CardServiceException(e.toString());
        }
    }
*/

    /* From BSI-03110 v1.1, B.2:
     *
     * <pre>
     * The following sequence of commands SHALL be used to implement Terminal Authentication:
     *  1. MSE:Set DST
     *  2. PSO:Verify Certificate
     *  3. MSE:Set AT
     *  4. Get Challenge
     *  5. External Authenticate
     * Steps 1 and 2 are repeated for every CV certificate to be verified
     * (CVCA Link Certificates, DV Certificate, IS Certificate).
     * </pre>
     */
    /**
     * Perform TA (Terminal Authentication) part of EAC (version 1). For details see
     * TR-03110 ver. 1.11. In short, we feed the sequence of terminal
     * certificates to the card for verification, get a challenge from the
     * card, sign it with terminal private key, and send back to the card
     * for verification.
     *
     * @param caReference reference issuer
     * @param terminalCertificates terminal certificate chain
     * @param terminalKey terminal private key
     * @param taAlg algorithm
     * @param chipAuthenticationResult the chip authentication result
     * @param documentNumber the document number
     *
     * @return the challenge from the card
     *
     * @throws CardServiceException on error
     */
   /* public static synchronized TAResult doTA(PassportService ps, CVCPrincipal caReference, List<CardVerifiableCertificate> terminalCertificates,
                                      PrivateKey terminalKey, String taAlg, CAResult chipAuthenticationResult, String documentNumber) throws CardServiceException {
        try {
            if (terminalCertificates == null || terminalCertificates.size() < 1) {
                throw new IllegalArgumentException("Need at least 1 certificate to perform TA, found: " + terminalCertificates);
            }

            byte[] caKeyHash = chipAuthenticationResult.getKeyHash();
            *//* The key hash that resulted from CA. *//*
            if (caKeyHash == null) {
                throw new IllegalArgumentException("CA key hash is null");
            }

            *//* FIXME: check that terminalCertificates holds a (inverted, i.e. issuer before subject) chain. *//*

            *//* Check if first cert is/has the expected CVCA, and remove it from chain if it is the CVCA. *//*
            CardVerifiableCertificate firstCert = terminalCertificates.get(0);
            CVCAuthorizationTemplate.Role firstCertRole = firstCert.getAuthorizationTemplate().getRole();
            if (CVCAuthorizationTemplate.Role.CVCA.equals(firstCertRole)) {
                CVCPrincipal firstCertHolderReference = firstCert.getHolderReference();
                if (caReference != null && !caReference.equals(firstCertHolderReference)) {
                    throw new CardServiceException("First certificate holds wrong authority, found " + firstCertHolderReference.getName() + ", expected " + caReference.getName());
                }
                if (caReference == null) {
                    caReference = firstCertHolderReference;
                }
                terminalCertificates.remove(0);
            }
            CVCPrincipal firstCertAuthorityReference = firstCert.getAuthorityReference();
            if (caReference != null && !caReference.equals(firstCertAuthorityReference)) {
                throw new CardServiceException("First certificate not signed by expected CA, found " + firstCertAuthorityReference.getName() + ",  expected " + caReference.getName());
            }
            if (caReference == null) {
                caReference = firstCertAuthorityReference;
            }

            *//* Check if the last cert is an IS cert. *//*
            CardVerifiableCertificate lastCert = terminalCertificates.get(terminalCertificates.size() - 1);
            CVCAuthorizationTemplate.Role lastCertRole = lastCert.getAuthorizationTemplate().getRole();
            if (!CVCAuthorizationTemplate.Role.IS.equals(lastCertRole)) {
                throw new CardServiceException("Last certificate in chain (" + lastCert.getHolderReference().getName() + ") does not have role IS, but has role " + lastCertRole);
            }
            CardVerifiableCertificate terminalCert = lastCert;

            *//* Have the MRTD check our chain. *//*
            for (CardVerifiableCertificate cert: terminalCertificates) {
                try {
                    CVCPrincipal authorityReference = cert.getAuthorityReference();

                    *//* Step 1: MSE:SetDST *//*
                    *//* Manage Security Environment: Set for verification: Digital Signature Template,
                     * indicate authority of cert to check.
                     *//*
                    byte[] authorityRefBytes = Util.wrapDO((byte) 0x83, authorityReference.getName().getBytes("ISO-8859-1"));
                    ps.sendMSESetDST(ps.getWrapper(), authorityRefBytes);

                    *//* Cert body is already in TLV format. *//*
                    byte[] body = cert.getCertBodyData();

                    *//* Signature not yet in TLV format, prefix it with tag and length. *//*
                    byte[] signature = cert.getSignature();
                    ByteArrayOutputStream sigOut = new ByteArrayOutputStream();
                    TLVOutputStream tlvSigOut = new TLVOutputStream(sigOut);
                    tlvSigOut.writeTag(TAG_CVCERTIFICATE_SIGNATURE);
                    tlvSigOut.writeValue(signature);
                    tlvSigOut.close();
                    signature = sigOut.toByteArray();

                    *//* Step 2: PSO:Verify Certificate *//*
                    ps.sendPSOExtendedLengthMode(ps.getWrapper(), body, signature);
                } catch (CardServiceException cse) {
                    throw cse;
                } catch (Exception e) {
                    *//* FIXME: Does this mean we failed to authenticate? -- MO *//*
                    throw new CardServiceException(e.getMessage());
                }
            }

            if (terminalKey == null) {
                throw new CardServiceException("No terminal key");
            }

            *//* Step 3: MSE Set AT *//*
            CVCPrincipal holderRef = terminalCert.getHolderReference();
            byte[] holderRefBytes = Util.wrapDO((byte) 0x83, holderRef.getName().getBytes("ISO-8859-1"));
            *//* Manage Security Environment: Set for external authentication: Authentication Template *//*
            ps.sendMSESetATExtAuth(ps.getWrapper(), holderRefBytes);

            *//* Step 4: send get challenge *//*
            byte[] rPICC = ps.sendGetChallenge(ps.getWrapper());

            *//* Step 5: external authenticate. *//*
            *//* FIXME: idPICC should be public key in case of PACE. See BSI TR 03110 v2.03 4.4. *//*
            byte[] idPICC = new byte[documentNumber.length() + 1];
            System.arraycopy(documentNumber.getBytes("ISO-8859-1"), 0, idPICC, 0, documentNumber.length());
            idPICC[idPICC.length - 1] = (byte)MRZInfo.checkDigit(documentNumber);

            ByteArrayOutputStream dtbs = new ByteArrayOutputStream();
            dtbs.write(idPICC);
            dtbs.write(rPICC);
            dtbs.write(caKeyHash);
            dtbs.close();
            byte[] dtbsBytes = dtbs.toByteArray();

            String sigAlg = terminalCert.getSigAlgName();
            if (sigAlg == null) {
                throw new IllegalStateException("ERROR: Could not determine signature algorithm for terminal certificate " + terminalCert.getHolderReference().getName());
            }
            Signature sig = Signature.getInstance(sigAlg);
            sig.initSign(terminalKey);
            sig.update(dtbsBytes);
            byte[] signedData = sig.sign();
            if (sigAlg.toUpperCase().endsWith("ECDSA")) {
                int keySize = ((org.bouncycastle.jce.interfaces.ECPrivateKey)terminalKey).getParameters().getCurve().getFieldSize() / 8;
                signedData = Util.getRawECDSASignature(signedData, keySize);
            }
            ps.sendMutualAuthenticate(ps.getWrapper(), signedData);

            //Not sure this should be here
            Field fld = PassportService.class.getDeclaredField("state");
            fld.setAccessible(true);
            fld.set(ps, Enum.valueOf((Class<Enum>) fld.getType(), "TA_AUTHENTICATED_STATE")) ;
            fld.set(ps, 5) ; //PassportService.TA_AUTHENTICATED_STATE)

            return new TAResult(chipAuthenticationResult, caReference, terminalCertificates, terminalKey, documentNumber, rPICC);
        } catch (CardServiceException cse) {
            throw cse;
        } catch (Exception e) {
            throw new CardServiceException(e.toString());
        }
    }

    public static List<CAResult> doChipAuthentication(PassportService ps) throws CardServiceException{
        InputStream is14 = null;
        List<CAResult> caResults = new ArrayList<>();
        try {
            is14 = ps.getInputStream(PassportService.EF_DG14);
            DG14File dg14 = (DG14File) LDSFileUtil.getLDSFile(PassportService.EF_DG14, is14);
            List<ChipAuthenticationPublicKeyInfo> chipAuthenticationPublicKeyInfos = dg14.getChipAuthenticationPublicKeyInfos();
            Iterator<ChipAuthenticationPublicKeyInfo> chipAuthenticationPublicKeyInfoIterator = chipAuthenticationPublicKeyInfos.iterator();
            while (chipAuthenticationPublicKeyInfoIterator.hasNext()){
                ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = chipAuthenticationPublicKeyInfoIterator.next();
                try {
                    Log.i("EMRTD", "Chip Authentication starting");
                    CAResult caResult = PassportNfcUtils.doCA(ps, BigInteger.valueOf(-1), chipAuthenticationPublicKeyInfo.getSubjectPublicKey());
                    Log.i("EMRTD", "Chip authentnication succeeded");
                    caResults.add(caResult);
                } catch(CardServiceException cse) {
                    cse.printStackTrace();
                    *//* NOTE: Failed? Too bad, try next public key. *//*
                    continue;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new CardServiceException("Chip authentication Failed");
        }finally {
            try {
                if(is14!=null){
                    is14.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return caResults;

    }


    public static List<TAResult> doEac(PassportService ps, String documentNumber, List<KeyStore> cvcaKeyStores) throws CardServiceException{

        InputStream isCvca=null;
        InputStream is14 = null;
        List<TAResult> taResults = new ArrayList<>();
        try {

            is14 = ps.getInputStream(PassportService.EF_DG14);
            DG14File dg14 = (DG14File) LDSFileUtil.getLDSFile(PassportService.EF_DG14, is14);

            isCvca = ps.getInputStream(PassportService.EF_CVCA);
            CVCAFile cvca = (CVCAFile) LDSFileUtil.getLDSFile(PassportService.EF_CVCA, isCvca);

            CVCPrincipal[] possibleCVCAReferences = new CVCPrincipal[]{ cvca.getCAReference(), cvca.getAltCAReference() };
            for (CVCPrincipal caReference: possibleCVCAReferences) {
                EACCredentials eacCredentials = getEACCredentials(caReference, cvcaKeyStores);
                if (eacCredentials == null) { continue; }

                PrivateKey privateKey = eacCredentials.getPrivateKey();
                Certificate[] chain = eacCredentials.getChain();
                List<CardVerifiableCertificate> terminalCerts = new ArrayList<CardVerifiableCertificate>(chain.length);
                for (Certificate c: chain) { terminalCerts.add((CardVerifiableCertificate)c); }

                List<ChipAuthenticationPublicKeyInfo> chipAuthenticationPublicKeyInfos = dg14.getChipAuthenticationPublicKeyInfos();
                Iterator<ChipAuthenticationPublicKeyInfo> chipAuthenticationPublicKeyInfoIterator = chipAuthenticationPublicKeyInfos.iterator();
                while (chipAuthenticationPublicKeyInfoIterator.hasNext()){
                    ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = chipAuthenticationPublicKeyInfoIterator.next();
                    try {
                        Log.i("EMRTD", "Chip Authentication starting");
                        CAResult caResult = PassportNfcUtils.doCA(ps, BigInteger.valueOf(-1), chipAuthenticationPublicKeyInfo.getSubjectPublicKey());
                        Log.i("EMRTD", "Chip authentnication succeeded");

                        Log.i("EMRTD", "Chip Terminal Authentication starting");
                        TAResult taResult = PassportNfcUtils.doTA(ps, caReference, terminalCerts, privateKey, null, caResult, documentNumber);
                        Log.i("EMRTD", "Chip Terminal authentnication succeeded");
                        taResults.add(taResult);
                    } catch(CardServiceException cse) {
                        cse.printStackTrace();
                        *//* NOTE: Failed? Too bad, try next public key. *//*
                        continue;
                    }
                }

                break;
            }


        }catch (Exception e){
            e.printStackTrace();
            throw new CardServiceException("EAC Failed");
        }finally {
            try {
                if(is14!=null){
                    is14.close();
                }
                if (isCvca != null) {
                    isCvca.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return taResults;

    }
*/


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





    private static EACCredentials getEACCredentials(CVCPrincipal caReference, List<KeyStore> cvcaStores) throws GeneralSecurityException {
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
