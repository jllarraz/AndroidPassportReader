package example.jllarraz.com.passportreader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.tlv.TLVOutputStream;

import org.jmrtd.ChipAuthenticationResult;
import org.jmrtd.DESedeSecureMessagingWrapper;
import org.jmrtd.PassportService;
import org.jmrtd.TerminalAuthenticationResult;
import org.jmrtd.Util;
import org.jmrtd.cert.CVCAuthorizationTemplate;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.MRZInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;

public class PassportNfcUtils {

    /**
     * Copy pasted, because original uses explicit cast to BouncyCastle key implementation, whereas we have a spongycastle one
     */
    public static synchronized ChipAuthenticationResult doCA(PassportService ps, BigInteger keyId, PublicKey publicKey) throws CardServiceException {
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

            SecretKey ksEnc = Util.deriveKey(secret, Util.ENC_MODE);
            SecretKey ksMac = Util.deriveKey(secret, Util.MAC_MODE);

            ps.setWrapper(new DESedeSecureMessagingWrapper(ksEnc, ksMac, 0L));
            Field fld = PassportService.class.getDeclaredField("state");
            fld.setAccessible(true);
            fld.set(ps, 4) ; //PassportService.CA_AUTHENTICATED_STATE)
            return new ChipAuthenticationResult(keyId, publicKey, keyHash, keyPair);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CardServiceException(e.toString());
        }
    }


    public static synchronized TerminalAuthenticationResult doTA(PassportService ps, CVCPrincipal caReference, List<CardVerifiableCertificate> terminalCertificates,
                                                          PrivateKey terminalKey, String taAlg, ChipAuthenticationResult chipAuthenticationResult, String documentNumber) throws CardServiceException {
        try {
            if (terminalCertificates == null || terminalCertificates.size() < 1) {
                throw new IllegalArgumentException("Need at least 1 certificate to perform TA, found: " + terminalCertificates);
            }

            byte[] caKeyHash = chipAuthenticationResult.getKeyHash();
            /* The key hash that resulted from CA. */
            if (caKeyHash == null) {
                throw new IllegalArgumentException("CA key hash is null");
            }

            /* FIXME: check that terminalCertificates holds a (inverted, i.e. issuer before subject) chain. */

            /* Check if first cert is/has the expected CVCA, and remove it from chain if it is the CVCA. */
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

            /* Check if the last cert is an IS cert. */
            CardVerifiableCertificate lastCert = terminalCertificates.get(terminalCertificates.size() - 1);
            CVCAuthorizationTemplate.Role lastCertRole = lastCert.getAuthorizationTemplate().getRole();
            if (!CVCAuthorizationTemplate.Role.IS.equals(lastCertRole)) {
                throw new CardServiceException("Last certificate in chain (" + lastCert.getHolderReference().getName() + ") does not have role IS, but has role " + lastCertRole);
            }
            CardVerifiableCertificate terminalCert = lastCert;

            int i = 0;
            /* Have the MRTD check our chain. */
            for (CardVerifiableCertificate cert: terminalCertificates) {
                try {
                    CVCPrincipal authorityReference = cert.getAuthorityReference();

                    /* Step 1: MSE:SetDST */
                    /* Manage Security Environment: Set for verification: Digital Signature Template,
                     * indicate authority of cert to check.
                     */
                    byte[] authorityRefBytes = Util.wrapDO((byte) 0x83, authorityReference.getName().getBytes("ISO-8859-1"));
                    ps.sendMSESetDST(ps.getWrapper(), authorityRefBytes);

                    /* Cert body is already in TLV format. */
                    byte[] body = cert.getCertBodyData();

                    /* Signature not yet in TLV format, prefix it with tag and length. */
                    byte[] signature = cert.getSignature();
                    ByteArrayOutputStream sigOut = new ByteArrayOutputStream();
                    TLVOutputStream tlvSigOut = new TLVOutputStream(sigOut);
                    tlvSigOut.writeTag(0x5F37); //TAG_CVCERTIFICATE_SIGNATURE);
                    tlvSigOut.writeValue(signature);
                    tlvSigOut.close();
                    signature = sigOut.toByteArray();

                    /* Step 2: PSO:Verify Certificate */
                    ps.sendPSOChainMode(ps.getWrapper(), body, signature);
                } catch (Exception cse) {
                    Log.w("FOO", String.valueOf(i));
                    throw cse;
                }
                i++;
            }

            if (terminalKey == null) {
                throw new CardServiceException("No terminal key");
            }

            /* Step 3: MSE Set AT */
            CVCPrincipal holderRef = terminalCert.getHolderReference();
            byte[] holderRefBytes = Util.wrapDO((byte) 0x83, holderRef.getName().getBytes("ISO-8859-1"));
            /* Manage Security Environment: Set for external authentication: Authentication Template */
            ps.sendMSESetATExtAuth(ps.getWrapper(), holderRefBytes);

            /* Step 4: send get challenge */
            byte[] rPICC = ps.sendGetChallenge(ps.getWrapper());

            /* Step 5: external authenticate. */
            /* FIXME: idPICC should be public key in case of PACE. See BSI TR 03110 v2.03 4.4. */
            byte[] idPICC = new byte[documentNumber.length() + 1];
            System.arraycopy(documentNumber.getBytes("ISO-8859-1"), 0, idPICC, 0, documentNumber.length());
            idPICC[idPICC.length - 1] = (byte) MRZInfo.checkDigit(documentNumber);

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
            Field fld = PassportService.class.getDeclaredField("state");
            fld.setAccessible(true);
            fld.set(ps, 5) ; //PassportService.TA_AUTHENTICATED_STATE)
            return new TerminalAuthenticationResult(chipAuthenticationResult, caReference, terminalCertificates, terminalKey, documentNumber, rPICC);
        } catch (CardServiceException cse) {
            throw cse;
        } catch (Exception e) {
            throw new CardServiceException(e.toString());
        }
    }


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

            return ImageUtil.decodeImage(
                    context, faceImageInfo.getMimeType(), inputStream);

        }
        throw new IOException("Unable to read Image");
    }
}
