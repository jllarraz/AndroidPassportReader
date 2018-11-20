package example.jllarraz.com.passportreader.utils;

import android.util.Log;

import net.sf.scuba.smartcards.CardServiceException;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.JMRTDSecurityProvider;
import org.jmrtd.MRTDTrustStore;
import org.jmrtd.PACEKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.Util;
import org.jmrtd.cert.CVCPrincipal;
import org.jmrtd.cert.CardVerifiableCertificate;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

import org.jmrtd.FeatureStatus;
import org.jmrtd.VerificationStatus;
import org.jmrtd.lds.AbstractTaggedLDSFile;
import org.jmrtd.lds.ActiveAuthenticationInfo;
import org.jmrtd.lds.CVCAFile;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.ChipAuthenticationInfo;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.COMFile;
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


public class PassportNFC {

    private static final String TAG = PassportNFC.class.getSimpleName();

    private static final Provider BC_PROVIDER = JMRTDSecurityProvider.getBouncyCastleProvider();

    private final static List<BACKey> EMPTY_TRIED_BAC_ENTRY_LIST = Collections.emptyList();
    private final static List<Certificate> EMPTY_CERTIFICATE_CHAIN = Collections.emptyList();

    /** The hash function for DG hashes. */
    private MessageDigest digest;

    private FeatureStatus featureStatus;
    private VerificationStatus verificationStatus;

    /* We use a cipher to help implement Active Authentication RSA with ISO9796-2 message recovery. */
    private transient Signature rsaAASignature;
    private transient MessageDigest rsaAADigest;
    private transient Cipher rsaAACipher;
    private transient Signature ecdsaAASignature;
    private transient MessageDigest ecdsaAADigest;

    private MRTDTrustStore trustManager;

    private PrivateKey docSigningPrivateKey;

    private CardVerifiableCertificate cvcaCertificate;

    private PrivateKey eacPrivateKey;

    private PrivateKey aaPrivateKey;

    private PassportService service;

    private Random random;


    private COMFile comFile = null;
    private SODFile sodFile = null;
    private DG1File dg1File = null;
    private DG2File dg2File = null;
    private DG3File dg3File = null;
    private DG5File dg5File = null;
    private DG7File dg7File = null;
    private DG11File dg11File = null;
    private DG12File dg12File = null;
    private DG14File dg14File = null;
    private DG15File dg15File = null;
    private CVCAFile cvcaFile = null;


    private PassportNFC() throws GeneralSecurityException {
        this.featureStatus = new FeatureStatus();
        this.verificationStatus = new VerificationStatus();

        this.random = new SecureRandom();

        rsaAADigest = MessageDigest.getInstance("SHA1"); /* NOTE: for output length measurement only. -- MO */
        rsaAASignature = Signature.getInstance("SHA1WithRSA/ISO9796-2", BC_PROVIDER);
        rsaAACipher = Cipher.getInstance("RSA/NONE/NoPadding");

        /* NOTE: These will be updated in doAA after caller has read ActiveAuthenticationSecurityInfo. */
        ecdsaAASignature = Signature.getInstance("SHA256withECDSA", BC_PROVIDER);
        ecdsaAADigest = MessageDigest.getInstance("SHA-256"); /* NOTE: for output length measurement only. -- MO */
    }

    
    /**
     * Creates a document by reading it from a service.
     *
     * @param ps the service to read from
     * @param trustManager the trust manager (CSCA, CVCA)
     * @param mrzInfo the BAC entries
     *
     * @throws CardServiceException on error
     * @throws GeneralSecurityException if certain security primitives are not supported
     */
    public PassportNFC(PassportService ps, MRTDTrustStore trustManager, MRZInfo mrzInfo) throws CardServiceException, GeneralSecurityException {
        this();
        if (ps == null) { throw new IllegalArgumentException("Service cannot be null"); }
        this.service = ps;
        this.trustManager = trustManager;

        boolean hasSAC;
        boolean isSACSucceeded = false;
        PACEResult paceResult = null;
        try {
            service.open();

            /* Find out whether this MRTD supports SAC. */
            try {
                Log.i(TAG, "Inspecting card access file");
                CardAccessFile cardAccessFile = new CardAccessFile(ps.getInputStream(PassportService.EF_CARD_ACCESS));
                Collection<SecurityInfo> securityInfos = cardAccessFile.getSecurityInfos();
                for (SecurityInfo securityInfo : securityInfos) {
                    if (securityInfo instanceof PACEInfo) {
                        featureStatus.setSAC(FeatureStatus.Verdict.PRESENT);
                    }
                }
            } catch (Exception e) {
                /* NOTE: No card access file, continue to test for BAC. */
                Log.i(TAG, "DEBUG: failed to get card access file: " + e.getMessage());
                e.printStackTrace();
            }

            hasSAC = featureStatus.hasSAC() == FeatureStatus.Verdict.PRESENT;

            if (hasSAC) {
                try {
                    paceResult = doPACE(ps, mrzInfo);
                    isSACSucceeded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "PACE failed, falling back to BAC");
                    isSACSucceeded = false;
                }
            }
            service.sendSelectApplet(isSACSucceeded);
        } catch (CardServiceException cse) {
            throw cse;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CardServiceException("Cannot open document. " + e.getMessage());
        }

        /* Find out whether this MRTD supports BAC. */
        try {
            /* Attempt to read EF.COM before BAC. */
            new COMFile(service.getInputStream(PassportService.EF_COM));

            if (isSACSucceeded) {
                verificationStatus.setSAC(VerificationStatus.Verdict.SUCCEEDED, "Succeeded");
                featureStatus.setBAC(FeatureStatus.Verdict.UNKNOWN);
                verificationStatus.setBAC(VerificationStatus.Verdict.NOT_CHECKED, "Using SAC, BAC not checked", EMPTY_TRIED_BAC_ENTRY_LIST);
            } else {
                /* We failed SAC, and we failed BAC. */
                featureStatus.setBAC(FeatureStatus.Verdict.NOT_PRESENT);
                verificationStatus.setBAC(VerificationStatus.Verdict.NOT_PRESENT, "Non-BAC document", EMPTY_TRIED_BAC_ENTRY_LIST);
            }
        } catch (Exception e) {
            Log.i(TAG, "Attempt to read EF.COM before BAC failed with: " + e.getMessage());
            featureStatus.setBAC(FeatureStatus.Verdict.PRESENT);
            verificationStatus.setBAC(VerificationStatus.Verdict.NOT_CHECKED, "BAC document", EMPTY_TRIED_BAC_ENTRY_LIST);
        }

        /* If we have to do BAC, try to do BAC. */
        boolean hasBAC = featureStatus.hasBAC() == FeatureStatus.Verdict.PRESENT;
        
        if (hasBAC && !(hasSAC && isSACSucceeded)) {
            BACKey bacKey = new BACKey(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());
            List<BACKey> triedBACEntries = new ArrayList<>();
            triedBACEntries.add(bacKey);
            try {
                doBAC(service, mrzInfo);
                verificationStatus.setBAC(VerificationStatus.Verdict.SUCCEEDED, "BAC succeeded with key " + bacKey, triedBACEntries);
            }catch (Exception e){
                verificationStatus.setBAC(VerificationStatus.Verdict.FAILED, "BAC failed", triedBACEntries);
            }
        }
        

        /* Pre-read these files that are always present. */
        
        Collection<Integer> dgNumbersAlreadyRead = new TreeSet<>();

        try {
            comFile = getComFile(ps);
            sodFile = getSodFile(ps);
            dg1File = getDG1File(ps);
            dgNumbersAlreadyRead.add(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Log.w(TAG, "Could not read file");
        }

        try{
            dg14File = getDG14File(ps);
        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            cvcaFile = getCVCAFile(ps);
        }catch (Exception e){
            e.printStackTrace();
        }

        /* Get the list of DGs from EF.SOd, we don't trust EF.COM. */
        List<Integer> dgNumbers = new ArrayList<>();
        if (sodFile != null) {
            dgNumbers.addAll(sodFile.getDataGroupHashes().keySet());
        } else if (comFile != null) {
            /* Get the list from EF.COM since we failed to parse EF.SOd. */
            Log.w(TAG, "Failed to get DG list from EF.SOd. Getting DG list from EF.COM.");
            int[] tagList = comFile.getTagList();
            dgNumbers.addAll(toDataGroupList(tagList));
        }
        Collections.sort(dgNumbers); /* NOTE: need to sort it, since we get keys as a set. */

        Log.i(TAG, "Found DGs: " + dgNumbers);

        Map<Integer, VerificationStatus.HashMatchResult> hashResults = verificationStatus.getHashResults();
        if (hashResults == null) {
            hashResults = new TreeMap<>();
        }

        if (sodFile != null) {
            /* Initial hash results: we know the stored hashes, but not the computed hashes yet. */
            Map<Integer, byte[]> storedHashes = sodFile.getDataGroupHashes();
            for (int dgNumber: dgNumbers) {
                byte[] storedHash = storedHashes.get(dgNumber);
                VerificationStatus.HashMatchResult hashResult = hashResults.get(dgNumber);
                if (hashResult != null) { continue; }
                if (dgNumbersAlreadyRead.contains(dgNumber)) {
                    hashResult = verifyHash(dgNumber);
                } else {
                    hashResult = new VerificationStatus.HashMatchResult(storedHash, null);
                }
                hashResults.put(dgNumber, hashResult);
            }
        }
        verificationStatus.setHT(VerificationStatus.Verdict.UNKNOWN, verificationStatus.getHTReason(), hashResults);

        /* Check EAC support by DG14 presence. */
        if (dgNumbers.contains(14)) {
            featureStatus.setEAC(FeatureStatus.Verdict.PRESENT);
            featureStatus.setCA(FeatureStatus.Verdict.PRESENT);
        } else {
            featureStatus.setEAC(FeatureStatus.Verdict.NOT_PRESENT);
            featureStatus.setCA(FeatureStatus.Verdict.NOT_PRESENT);
        }

        boolean hasCA = featureStatus.hasCA() == FeatureStatus.Verdict.PRESENT;
        if(hasCA){
            try {
                List<EACCAResult> eaccaResults = doEACCA(ps, mrzInfo, dg14File, sodFile);
                verificationStatus.setCA(VerificationStatus.Verdict.SUCCEEDED, "EAC succeeded", eaccaResults.get(0));
            }catch (Exception e){
                verificationStatus.setCA(VerificationStatus.Verdict.FAILED, "CA Failed", null);
            }
        }

        boolean hasEAC = featureStatus.hasEAC() == FeatureStatus.Verdict.PRESENT;
        List<KeyStore> cvcaKeyStores = trustManager.getCVCAStores();
        if (hasEAC && cvcaKeyStores != null && cvcaKeyStores.size() > 0 && verificationStatus.getCA() == VerificationStatus.Verdict.SUCCEEDED) {
            try {
                List<EACTAResult> eactaResults = doEACTA(ps, mrzInfo, cvcaFile, paceResult, verificationStatus.getCAResult(), cvcaKeyStores);
                verificationStatus.setEAC(VerificationStatus.Verdict.SUCCEEDED, "EAC succeeded", eactaResults.get(0));
            }catch (Exception e){
                e.printStackTrace();
                verificationStatus.setEAC(VerificationStatus.Verdict.FAILED, "EAC Failed", null);
            }
            dgNumbersAlreadyRead.add(14);
        }

        /* Check AA support by DG15 presence. */
        if (dgNumbers.contains(15)) {
            featureStatus.setAA(FeatureStatus.Verdict.PRESENT);
        } else {
            featureStatus.setAA(FeatureStatus.Verdict.NOT_PRESENT);
        }
        boolean hasAA = featureStatus.hasAA() == FeatureStatus.Verdict.PRESENT;
        if (hasAA) {
            try {
                dg15File = getDG15File(ps);
                dgNumbersAlreadyRead.add(15);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.w(TAG, "Could not read file");
            } catch (Exception e) {
                verificationStatus.setAA(VerificationStatus.Verdict.NOT_CHECKED, "Failed to read DG15");
            }
        } else {
            /* Feature status says: no AA, so verification status should say: no AA. */
            verificationStatus.setAA(VerificationStatus.Verdict.NOT_PRESENT, "AA is not supported");
        }

        
        try{
            dg2File=getDG2File(ps);
        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            dg3File=getDG3File(ps);
        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            dg5File=getDG5File(ps);
        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            dg7File=getDG7File(ps);
        }catch (Exception e){
            e.printStackTrace();
        }


        try{
            dg11File=getDG11File(ps);
        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            dg12File=getDG12File(ps);
        }catch (Exception e){
            e.printStackTrace();
        }
        
    }

    public COMFile getComFile() {
        return comFile;
    }

    public SODFile getSodFile() {
        return sodFile;
    }

    public DG1File getDg1File() {
        return dg1File;
    }

    public DG2File getDg2File() {
        return dg2File;
    }

    public DG3File getDg3File() {
        return dg3File;
    }

    public DG5File getDg5File() {
        return dg5File;
    }

    public DG7File getDg7File() {
        return dg7File;
    }

    public DG11File getDg11File() {
        return dg11File;
    }

    public DG12File getDg12File() {
        return dg12File;
    }

    public DG14File getDg14File() {
        return dg14File;
    }

    public DG15File getDg15File() {
        return dg15File;
    }

    public CVCAFile getCvcaFile() {
        return cvcaFile;
    }


    /**
     * Sets the document signing private key.
     *
     * @param docSigningPrivateKey a private key
     */
    public void setDocSigningPrivateKey(PrivateKey docSigningPrivateKey) {
        this.docSigningPrivateKey = docSigningPrivateKey;
        updateCOMSODFile(null);
    }

    /**
     * Gets the CVCA certificate.
     *
     * @return a CV certificate or null
     */
    public CardVerifiableCertificate getCVCertificate() {
        return cvcaCertificate;
    }

    /**
     * Sets the CVCA certificate.
     *
     * @param cert the CV certificate
     */
    public void setCVCertificate(CardVerifiableCertificate cert) {
        this.cvcaCertificate = cert;
        try {
            CVCAFile cvcaFile = new CVCAFile(PassportService.EF_CVCA, cvcaCertificate.getHolderReference().getName());
            putFile(PassportService.EF_CVCA, cvcaFile.getEncoded());
        } catch (CertificateException ce) {
            ce.printStackTrace();
        }
    }

    /**
     * Gets the document signing private key, or null if not present.
     *
     * @return a private key or null
     */
    public PrivateKey getDocSigningPrivateKey() {
        return docSigningPrivateKey;
    }

    /**
     * Sets the document signing certificate.
     *
     * @param docSigningCertificate a certificate
     */
    public void setDocSigningCertificate(X509Certificate docSigningCertificate) {
        updateCOMSODFile(docSigningCertificate);
    }

    /**
     * Gets the CSCA, CVCA trust store.
     *
     * @return the trust store in use
     */
    public MRTDTrustStore getTrustManager() {
        return trustManager;
    }

    /**
     * Gets the private key for EAC, or null if not present.
     *
     * @return a private key or null
     */
    public PrivateKey getEACPrivateKey() {
        return eacPrivateKey;
    }

    /**
     * Sets the private key for EAC.
     *
     * @param eacPrivateKey a private key
     */
    public void setEACPrivateKey(PrivateKey eacPrivateKey) {
        this.eacPrivateKey = eacPrivateKey;
    }

    /**
     * Sets the public key for EAC.
     *
     * @param eacPublicKey a public key
     */
    public void setEACPublicKey(PublicKey eacPublicKey) {
        ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo = new ChipAuthenticationPublicKeyInfo(eacPublicKey);
        DG14File dg14File = new DG14File(Arrays.asList(new SecurityInfo[] { chipAuthenticationPublicKeyInfo }));
        putFile(PassportService.EF_DG14, dg14File.getEncoded());
    }

    /**
     * Gets the private key for AA, or null if not present.
     *
     * @return a private key or null
     */
    public PrivateKey getAAPrivateKey() {
        return aaPrivateKey;
    }

    /**
     * Sets the private key for AA.
     *
     * @param aaPrivateKey a private key
     */
    public void setAAPrivateKey(PrivateKey aaPrivateKey) {
        this.aaPrivateKey = aaPrivateKey;
    }

    /**
     * Sets the public key for AA.
     *
     * @param aaPublicKey a public key
     */
    public void setAAPublicKey(PublicKey aaPublicKey) {
        DG15File dg15file = new DG15File(aaPublicKey);
        putFile(PassportService.EF_DG15, dg15file.getEncoded());
    }

    /**
     * Gets the supported features (such as: BAC, AA, EAC) as
     * discovered during initialization of this document.
     *
     * @return the supported features
     *
     * @since 0.4.9
     */
    public FeatureStatus getFeatures() {
        /* The feature status has been created in constructor. */
        return featureStatus;
    }

    /**
     * Gets the verification status thus far.
     *
     * @return the verification status
     *
     * @since 0.4.9
     */
    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    /**
     * Verifies the document using the security related mechanisms.
     * Convenience method.
     *
     * @return the security status
     */
    public VerificationStatus verifySecurity() {
        /* NOTE: Since 0.4.9 verifyAA and verifyEAC were removed. AA is always checked as part of the prelude.
         * (EDIT: For debugging it's back here again, see below...)
         */
        /* NOTE: We could also move verifyDS and verifyCS to prelude. */
        /* NOTE: COM SOd consistency check ("Jeroen van Beek sanity check") is implicit now, we work from SOd, ignoring COM. */

        /* Verify whether the Document Signing Certificate is signed by a Trust Anchor in our CSCA store. */
        verifyCS();

        /* Verify whether hashes in EF.SOd signed with document signer certificate. */
        verifyDS();

        /* Verify hashes. */
        verifyHT();

        /* DEBUG: apparently it matters where we do AA, in prelude or in the end?!?! -- MO */
        if (service != null && dg15File!=null) {
            verifyAA();
        }

        return verificationStatus;
    }

    /**
     * Inserts a file into this document, and updates EF_COM and EF_SOd accordingly.
     *
     * @param fid the FID of the new file
     * @param bytes the contents of the new file
     */
    private void putFile(short fid, byte[] bytes) {
        if (bytes == null) { return; }
        try {
            //lds.add(fid, new ByteArrayInputStream(bytes), bytes.length);
            // FIXME: is this necessary?
            if(fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != PassportService.EF_CVCA) {
                updateCOMSODFile(null);
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        verificationStatus.setAll(VerificationStatus.Verdict.UNKNOWN, "Unknown"); // FIXME: why all?
    }

    /**
     * Updates EF_COM and EF_SOd using a new document signing certificate.
     *
     * @param newCertificate a certificate
     */
    private void updateCOMSODFile(X509Certificate newCertificate) {
        try {
            String digestAlg = sodFile.getDigestAlgorithm();
            String signatureAlg = sodFile.getDigestEncryptionAlgorithm();
            X509Certificate cert = newCertificate != null ? newCertificate : sodFile.getDocSigningCertificate();
            byte[] signature = sodFile.getEncryptedDigest();
            Map<Integer, byte[]> dgHashes = new TreeMap<>();

            List<Integer> dgFids = LDSFileUtil.getDataGroupNumbers(sodFile);
            MessageDigest digest;
            digest = MessageDigest.getInstance(digestAlg);
            for (int fid : dgFids) {
                if (fid != PassportService.EF_COM && fid != PassportService.EF_SOD && fid != PassportService.EF_CVCA) {
                    AbstractTaggedLDSFile dg = getDG(fid);
                    if(dg==null){
                        Log.w(TAG, "Could not get input stream for " + Integer.toHexString(fid)); continue;
                    }
                    byte tag = dg.getEncoded()[0];
                    dgHashes.put(LDSFileUtil.lookupDataGroupNumberByTag(tag), digest.digest(dg.getEncoded()));
                    comFile.insertTag(tag & 0xFF);
                }
            }
            if(docSigningPrivateKey != null) {
                sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, docSigningPrivateKey, cert);
            } else {
                sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, signature, cert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    ///////////////////////////////////

    /** Check active authentication. */
    private void verifyAA() {
        if (dg15File == null || service == null) {
            verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed");
            return;
        }

        try {
            
            PublicKey pubKey = dg15File.getPublicKey();
            String pubKeyAlgorithm = pubKey.getAlgorithm();
            String digestAlgorithm = "SHA1";
            String signatureAlgorithm = "SHA1WithRSA/ISO9796-2";
            if ("EC".equals(pubKeyAlgorithm) || "ECDSA".equals(pubKeyAlgorithm)) {
                
              //  List activeAuthenticationInfos = dg14File.getActiveAuthenticationInfos();
                List<ActiveAuthenticationInfo> activeAuthenticationInfoList = new ArrayList<>();
                Collection<SecurityInfo> securityInfos = dg14File.getSecurityInfos();
                for (SecurityInfo securityInfo : securityInfos) {
                    if (securityInfo instanceof ActiveAuthenticationInfo) {
                        activeAuthenticationInfoList.add((ActiveAuthenticationInfo) securityInfo);
                    }
                }


                int activeAuthenticationInfoCount = activeAuthenticationInfoList.size();
                if (activeAuthenticationInfoCount < 1) {
                    verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "Found no active authentication info in EF.DG14");
                    return;
                } else if (activeAuthenticationInfoCount > 1) {
                    Log.w(TAG, "Found " + activeAuthenticationInfoCount + " in EF.DG14, expected 1.");
                }
                ActiveAuthenticationInfo activeAuthenticationInfo = activeAuthenticationInfoList.get(0);

                String signatureAlgorithmOID = activeAuthenticationInfo.getSignatureAlgorithmOID();

                signatureAlgorithm = ActiveAuthenticationInfo.lookupMnemonicByOID(signatureAlgorithmOID);

                digestAlgorithm = Util.inferDigestAlgorithmFromSignatureAlgorithm(signatureAlgorithm);
            }
            int challengeLength = 8;
            byte[] challenge = new byte[challengeLength];
            random.nextBytes(challenge);
            AAResult aaResult = service.doAA(dg15File.getPublicKey(), sodFile.getDigestAlgorithm(), sodFile.getSignerInfoDigestAlgorithm(), challenge);
            if (verifyAA(pubKey, digestAlgorithm, signatureAlgorithm, challenge, aaResult.getResponse())) {
                verificationStatus.setAA(VerificationStatus.Verdict.SUCCEEDED, "AA succeeded");
            } else {
                verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed due to signature failure");
            }
        } catch (CardServiceException cse) {
            cse.printStackTrace();
            verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed due to exception");
        } catch (Exception e) {
            Log.e(TAG, "DEBUG: this exception wasn't caught in verification logic (< 0.4.8) -- MO 3. Type is " + e.getClass().getCanonicalName());
            e.printStackTrace();
            verificationStatus.setAA(VerificationStatus.Verdict.FAILED, "AA failed due to exception");
        }
    }

    private boolean verifyAA(PublicKey publicKey, String digestAlgorithm, String signatureAlgorithm, byte[] challenge, byte[] response) throws CardServiceException {
        try {
            String pubKeyAlgorithm = publicKey.getAlgorithm();
            if ("RSA".equals(pubKeyAlgorithm)) {
                /* FIXME: check that digestAlgorithm = "SHA1" in this case, check (and re-initialize) rsaAASignature (and rsaAACipher). */
                Log.w(TAG, "Unexpected algorithms for RSA AA: "
                        + "digest algorithm = " + (digestAlgorithm == null ? "null" : digestAlgorithm)
                        + ", signature algorithm = " + (signatureAlgorithm == null ? "null" : signatureAlgorithm));

                rsaAADigest = MessageDigest.getInstance(digestAlgorithm); /* NOTE: for output length measurement only. -- MO */
                rsaAASignature = Signature.getInstance(signatureAlgorithm, BC_PROVIDER);

                RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
                rsaAACipher.init(Cipher.DECRYPT_MODE, rsaPublicKey);
                rsaAASignature.initVerify(rsaPublicKey);

                int digestLength = rsaAADigest.getDigestLength(); /* SHA1 should be 20 bytes = 160 bits */
                if ((digestLength != 20)) throw new AssertionError();
                byte[] plaintext = rsaAACipher.doFinal(response);
                byte[] m1 = Util.recoverMessage(digestLength, plaintext);
                rsaAASignature.update(m1);
                rsaAASignature.update(challenge);
                return rsaAASignature.verify(response);
            } else if ("EC".equals(pubKeyAlgorithm) || "ECDSA".equals(pubKeyAlgorithm)) {
                ECPublicKey ecdsaPublicKey = (ECPublicKey)publicKey;

                if (ecdsaAASignature == null || signatureAlgorithm != null && !signatureAlgorithm.equals(ecdsaAASignature.getAlgorithm())) {
                    Log.w(TAG, "Re-initializing ecdsaAASignature with signature algorithm " + signatureAlgorithm);
                    ecdsaAASignature = Signature.getInstance(signatureAlgorithm);
                }
                if (ecdsaAADigest == null || digestAlgorithm != null && !digestAlgorithm.equals(ecdsaAADigest.getAlgorithm())) {
                    Log.w(TAG, "Re-initializing ecdsaAADigest with digest algorithm " + digestAlgorithm);
                    ecdsaAADigest = MessageDigest.getInstance(digestAlgorithm);
                }

                ecdsaAASignature.initVerify(ecdsaPublicKey);

                if (response.length % 2 != 0) {
                    Log.w(TAG, "Active Authentication response is not of even length");
                }

                int l = response.length / 2;
                BigInteger r = Util.os2i(response, 0, l);
                BigInteger s = Util.os2i(response, l, l);

                ecdsaAASignature.update(challenge);

                try {

                    ASN1Sequence asn1Sequence = new DERSequence(new ASN1Encodable[] { new ASN1Integer(r), new ASN1Integer(s) });
                    return ecdsaAASignature.verify(asn1Sequence.getEncoded());
                } catch (IOException ioe) {
                    Log.e(TAG, "Unexpected exception during AA signature verification with ECDSA");
                    ioe.printStackTrace();
                    return false;
                }
            } else {
                Log.e(TAG, "Unsupported AA public key type " + publicKey.getClass().getSimpleName());
                return false;
            }
        } catch (IllegalArgumentException | GeneralSecurityException iae) {
            // iae.printStackTrace();
            throw new CardServiceException(iae.toString());
        }
    }

    /**
     * Checks the security object's signature.
     *
     * TODO: Check the cert stores (notably PKD) to fetch document signer certificate (if not embedded in SOd) and check its validity before checking the signature.
     */
    private void verifyDS() {
        try {
            verificationStatus.setDS(VerificationStatus.Verdict.UNKNOWN, "Unknown");

            /* Check document signing signature. */
            X509Certificate docSigningCert = sodFile.getDocSigningCertificate();
            if (docSigningCert == null) {
                Log.w(TAG, "Could not get document signer certificate from EF.SOd");
                // FIXME: We search for it in cert stores. See note at verifyCS.
                // X500Principal issuer = sod.getIssuerX500Principal();
                // BigInteger serialNumber = sod.getSerialNumber();
            }
            if (checkDocSignature(docSigningCert)) {
                verificationStatus.setDS(VerificationStatus.Verdict.SUCCEEDED, "Signature checked");
            } else {
                verificationStatus.setDS(VerificationStatus.Verdict.FAILED, "Signature incorrect");
            }
        } catch (NoSuchAlgorithmException nsae) {
            verificationStatus.setDS(VerificationStatus.Verdict.FAILED, "Unsupported signature algorithm");
            return; /* NOTE: Serious enough to not perform other checks, leave method. */
        } catch (Exception e) {
            e.printStackTrace();
            verificationStatus.setDS(VerificationStatus.Verdict.FAILED, "Unexpected exception");
            return; /* NOTE: Serious enough to not perform other checks, leave method. */
        }
    }

    /**
     * Checks the certificate chain.
     */
    private void verifyCS() {
        try {

            List<Certificate> chain = new ArrayList<Certificate>();

            if (sodFile == null) {
                verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "Unable to build certificate chain", chain);
                return;
            }

            /* Get doc signing certificate and issuer info. */
            X509Certificate docSigningCertificate = null;
            X500Principal sodIssuer = null;
            BigInteger sodSerialNumber = null;
            try {
                sodIssuer = sodFile.getIssuerX500Principal();
                sodSerialNumber = sodFile.getSerialNumber();
                docSigningCertificate = sodFile.getDocSigningCertificate();
            }  catch (Exception e) {
                Log.w(TAG, "Error getting document signing certificate: " + e.getMessage());
                // FIXME: search for it in cert stores?
            }

            if (docSigningCertificate != null) {
                chain.add(docSigningCertificate);
            } else {
                Log.w(TAG, "Error getting document signing certificate from EF.SOd");
            }

            /* Get trust anchors. */
            List<CertStore> cscaStores = trustManager.getCSCAStores();
            if (cscaStores == null || cscaStores.size() <= 0) {
                Log.w(TAG, "No CSCA certificate stores found.");
                verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "No CSCA certificate stores found", chain);
            }
            Set<TrustAnchor> cscaTrustAnchors = trustManager.getCSCAAnchors();
            if (cscaTrustAnchors == null || cscaTrustAnchors.size() <= 0) {
                Log.w(TAG, "No CSCA trust anchors found.");
                verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "No CSCA trust anchors found", chain);
            }

            /* Optional internal EF.SOd consistency check. */
            if (docSigningCertificate != null) {
                X500Principal docIssuer = docSigningCertificate.getIssuerX500Principal();
                if (sodIssuer != null && !sodIssuer.equals(docIssuer)) {
                    Log.e(TAG, "Security object issuer principal is different from embedded DS certificate issuer!");
                }
                BigInteger docSerialNumber = docSigningCertificate.getSerialNumber();
                if (sodSerialNumber != null && !sodSerialNumber.equals(docSerialNumber)) {
                    Log.w(TAG, "Security object serial number is different from embedded DS certificate serial number!");
                }
            }

            /* Run PKIX algorithm to build chain to any trust anchor. Add certificates to our chain. */
            List<Certificate> pkixChain = PassportNfcUtils.getCertificateChain(docSigningCertificate, sodIssuer, sodSerialNumber, cscaStores, cscaTrustAnchors);
            if (pkixChain == null) {
                verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "Could not build chain to trust anchor (pkixChain == null)", chain);
                return;
            }

            for (Certificate certificate: pkixChain) {
                if (certificate.equals(docSigningCertificate)) { continue; } /* Ignore DS certificate, which is already in chain. */
                chain.add(certificate);
            }

            int chainDepth = chain.size();
            if (chainDepth <= 1) {
                verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "Could not build chain to trust anchor", chain);
                return;
            }
            if (chainDepth > 1 && verificationStatus.getCS().equals(VerificationStatus.Verdict.UNKNOWN)) {
                verificationStatus.setCS(VerificationStatus.Verdict.SUCCEEDED, "Found a chain to a trust anchor", chain);
            }

        } catch (Exception e) {
            e.printStackTrace();
            verificationStatus.setCS(VerificationStatus.Verdict.FAILED, "Signature failed", EMPTY_CERTIFICATE_CHAIN);
        }
    }

    /**
     * Checks hashes in the SOd correspond to hashes we compute.
     */
    private void verifyHT() {
        /* Compare stored hashes to computed hashes. */
        Map<Integer, VerificationStatus.HashMatchResult> hashResults = verificationStatus.getHashResults();
        if (hashResults == null) {
            hashResults = new TreeMap<>();
        }

        if (sodFile == null) {
            verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "No SOd", hashResults);
            return;
        }

        Map<Integer, byte[]> storedHashes = sodFile.getDataGroupHashes();
        for (int dgNumber: storedHashes.keySet()) {
            verifyHash(dgNumber, hashResults);
        }
        if (verificationStatus.getHT().equals(VerificationStatus.Verdict.UNKNOWN)) {
            verificationStatus.setHT(VerificationStatus.Verdict.SUCCEEDED, "All hashes match", hashResults);
        } else {
            /* Update storedHashes and computedHashes. */
            verificationStatus.setHT(verificationStatus.getHT(), verificationStatus.getHTReason(), hashResults);
        }
    }

    private VerificationStatus.HashMatchResult verifyHash(int dgNumber) {
        Map<Integer, VerificationStatus.HashMatchResult> hashResults = verificationStatus.getHashResults();
        if (hashResults == null) {
            hashResults = new TreeMap<>();
        }
        return verifyHash(dgNumber, hashResults);
    }

    /**
     * Verifies the hash for the given datagroup.
     * Note that this will block until all bytes of the datagroup
     * are loaded.
     *
     * @param dgNumber
     *
     * @param hashResults the hashtable status to update
     */
    private VerificationStatus.HashMatchResult verifyHash(int dgNumber, Map<Integer, VerificationStatus.HashMatchResult> hashResults) {
        short fid = LDSFileUtil.lookupFIDByTag(LDSFileUtil.lookupTagByDataGroupNumber(dgNumber));



        /* Get the stored hash for the DG. */
        byte[] storedHash = null;
        try {
            Map<Integer, byte[]> storedHashes = sodFile.getDataGroupHashes();
            storedHash = storedHashes.get(dgNumber);
        } catch(Exception e) {
            verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "DG" + dgNumber + " failed, could not get stored hash", hashResults);
            return null;
        }

        /* Initialize hash. */
        String digestAlgorithm = sodFile.getDigestAlgorithm();
        try {
            digest = getDigest(digestAlgorithm);
        } catch (NoSuchAlgorithmException nsae) {
            verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "Unsupported algorithm \"" + digestAlgorithm + "\"", null);
            return null; // DEBUG -- MO
        }

        /* Read the DG. */
        byte[] dgBytes = null;
        try {
            /*InputStream dgIn = null;
            int length = lds.getLength(fid);
            if (length > 0) {
                dgBytes = new byte[length];
                dgIn = lds.getInputStream(fid);
                DataInputStream dgDataIn = new DataInputStream(dgIn);
                dgDataIn.readFully(dgBytes);
            }*/

            AbstractTaggedLDSFile abstractTaggedLDSFile = getDG(fid);
            if(abstractTaggedLDSFile!=null){
                dgBytes = abstractTaggedLDSFile.getEncoded();
            }

            if (abstractTaggedLDSFile == null && (verificationStatus.getEAC() != VerificationStatus.Verdict.SUCCEEDED) && (fid == PassportService.EF_DG3 || fid == PassportService.EF_DG4)) {
                Log.w(TAG, "Skipping DG" + dgNumber + " during HT verification because EAC failed.");
                VerificationStatus.HashMatchResult hashResult = new VerificationStatus.HashMatchResult(storedHash, null);
                hashResults.put(dgNumber, hashResult);
                return hashResult;
            }
            if (abstractTaggedLDSFile == null) {
                Log.w(TAG, "Skipping DG" + dgNumber + " during HT verification because file could not be read.");
                VerificationStatus.HashMatchResult hashResult = new VerificationStatus.HashMatchResult(storedHash, null);
                hashResults.put(dgNumber, hashResult);
                return hashResult;
            }

        } catch(Exception e) {
            VerificationStatus.HashMatchResult hashResult = new VerificationStatus.HashMatchResult(storedHash, null);
            hashResults.put(dgNumber, hashResult);
            verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "DG" + dgNumber + " failed due to exception", hashResults);
            return hashResult;
        }

        /* Compute the hash and compare. */
        try {
            byte[] computedHash = digest.digest(dgBytes);
            VerificationStatus.HashMatchResult hashResult = new VerificationStatus.HashMatchResult(storedHash, computedHash);
            hashResults.put(dgNumber, hashResult);

            if (!Arrays.equals(storedHash, computedHash)) {
                verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "Hash mismatch", hashResults);
            }

            return hashResult;
        } catch (Exception ioe) {
            VerificationStatus.HashMatchResult hashResult = new VerificationStatus.HashMatchResult(storedHash, null);
            hashResults.put(dgNumber, hashResult);
            verificationStatus.setHT(VerificationStatus.Verdict.FAILED, "Hash failed due to exception", hashResults);
            return hashResult;
        }
    }



    private MessageDigest getDigest(String digestAlgorithm) throws NoSuchAlgorithmException {
        if (digest != null) {
            digest.reset();
            return digest;
        }
        Log.i(TAG, "Using hash algorithm " + digestAlgorithm);
        if (Security.getAlgorithms("MessageDigest").contains(digestAlgorithm)) {
            digest = MessageDigest.getInstance(digestAlgorithm);
        } else {
            digest = MessageDigest.getInstance(digestAlgorithm, BC_PROVIDER);
        }
        return digest;
    }
    
    private AbstractTaggedLDSFile getDG(int dg){
        switch (dg){
            case 1:{
                return dg1File;
            }
            case 2:{
                return dg2File;
            }
            case 3:{
                return dg3File;
            }
            case 5:{
                return dg5File;
            }
            case 7:{
                return dg7File;
            }
            case 11:{
                return dg11File;
            }
            case 12:{
                return dg12File;
            }
            case 14:{
                return dg14File;
            }
            case 15:{
                return dg15File;
            }
            default:{
                return null;
            }

        }

    }


    /**
     * Verifies the signature over the contents of the security object.
     * Clients can also use the accessors of this class and check the
     * validity of the signature for themselves.
     *
     * See RFC 3369, Cryptographic Message Syntax, August 2002,
     * Section 5.4 for details.
     *
     * @param docSigningCert the certificate to use
     *        (should be X509 certificate)
     *
     * @return status of the verification
     *
     * @throws GeneralSecurityException if something goes wrong
     */
    /* FIXME: move this out of lds package. */
    private boolean checkDocSignature(Certificate docSigningCert) throws GeneralSecurityException {
        byte[] eContent = sodFile.getEContent();
        byte[] signature = sodFile.getEncryptedDigest();

        String digestEncryptionAlgorithm = null;
        try {
            digestEncryptionAlgorithm = sodFile.getDigestEncryptionAlgorithm();
        } catch (Exception e) {
            digestEncryptionAlgorithm = null;
        }

        /*
         * For the cases where the signature is simply a digest (haven't seen a passport like this,
         * thus this is guessing)
         */
        if (digestEncryptionAlgorithm == null) {
            String digestAlg = sodFile.getSignerInfoDigestAlgorithm();
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance(digestAlg);
            } catch (Exception e) {
                digest = MessageDigest.getInstance(digestAlg, BC_PROVIDER);
            }
            digest.update(eContent);
            byte[] digestBytes = digest.digest();
            return Arrays.equals(digestBytes, signature);
        }


        /* For RSA_SA_PSS
         *    1. the default hash is SHA1,
         *    2. The hash id is not encoded in OID
         * So it has to be specified "manually".
         */
        if ("SSAwithRSA/PSS".equals(digestEncryptionAlgorithm)) {
            String digestAlg = sodFile.getSignerInfoDigestAlgorithm();
            digestEncryptionAlgorithm = digestAlg.replace("-", "") + "withRSA/PSS";
        }

        if ("RSA".equals(digestEncryptionAlgorithm)) {
            String digestJavaString = sodFile.getSignerInfoDigestAlgorithm();
            digestEncryptionAlgorithm = digestJavaString.replace("-", "") + "withRSA";
        }

        Log.i(TAG, "digestEncryptionAlgorithm = " + digestEncryptionAlgorithm);

        Signature sig = null;

        sig = Signature.getInstance(digestEncryptionAlgorithm, BC_PROVIDER);
        if(digestEncryptionAlgorithm.endsWith("withRSA/PSS")){
            int saltLength = findSaltRSA_PSS(digestEncryptionAlgorithm, docSigningCert, eContent, signature);//Unknown salt so we try multiples until we get a success or failure
            MGF1ParameterSpec mgf1ParameterSpec = new MGF1ParameterSpec("SHA-256");
            PSSParameterSpec pssParameterSpec = new PSSParameterSpec("SHA-256", "MGF1",mgf1ParameterSpec , saltLength, 1);
            sig.setParameter(pssParameterSpec);
        }
        /*try {
            sig = Signature.getInstance(digestEncryptionAlgorithm);
        } catch (Exception e) {
            sig = Signature.getInstance(digestEncryptionAlgorithm, BC_PROVIDER);
        }*/
        sig.initVerify(docSigningCert);
        sig.update(eContent);
        return sig.verify(signature);
    }


    private int findSaltRSA_PSS(String digestEncryptionAlgorithm, Certificate docSigningCert, byte[] eContent, byte[] signature){
        //Using brute force
        for(int i=0;i<513;i++) {
            try {
                Signature sig = null;

                sig = Signature.getInstance(digestEncryptionAlgorithm, BC_PROVIDER);
                if (digestEncryptionAlgorithm.endsWith("withRSA/PSS")) {
                    int saltLength = i;
                    MGF1ParameterSpec mgf1ParameterSpec = new MGF1ParameterSpec("SHA-256");
                    PSSParameterSpec pssParameterSpec = new PSSParameterSpec("SHA-256", "MGF1", mgf1ParameterSpec, saltLength, 1);
                    sig.setParameter(pssParameterSpec);
                }

                sig.initVerify(docSigningCert);
                sig.update(eContent);
                boolean verify = sig.verify(signature);
                if(verify){
                    return i;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;//Unable to find it
    }



    ////////////////////////////

    private PACEResult doPACE(PassportService ps, MRZInfo mrzInfo) throws IOException, CardServiceException, GeneralSecurityException {
        PACEResult paceResult = null;
        InputStream isCardAccessFile = null;
        try {
            BACKeySpec bacKey = new BACKey(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());
            PACEKeySpec paceKeySpec = PACEKeySpec.createMRZKey(bacKey);
            isCardAccessFile = ps.getInputStream(PassportService.EF_CARD_ACCESS);

            CardAccessFile cardAccessFile = new CardAccessFile(isCardAccessFile);
            Collection<SecurityInfo> securityInfos = cardAccessFile.getSecurityInfos();
            SecurityInfo securityInfo = securityInfos.iterator().next();
            List<PACEInfo> paceInfos = new ArrayList<>();
            if (securityInfo instanceof PACEInfo) {
                paceInfos.add((PACEInfo) securityInfo);
            }

            if (paceInfos.size() > 0) {
                PACEInfo paceInfo = paceInfos.iterator().next();
                paceResult = ps.doPACE(paceKeySpec, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()));
            }
        }
        finally {
            if(isCardAccessFile!=null){
                isCardAccessFile.close();
                isCardAccessFile = null;
            }
        }
        return paceResult;
    }

    private BACResult doBAC(PassportService ps, MRZInfo mrzInfo) throws CardServiceException {
        BACKeySpec bacKey = new BACKey(mrzInfo.getDocumentNumber(), mrzInfo.getDateOfBirth(), mrzInfo.getDateOfExpiry());
        return ps.doBAC(bacKey);
    }


    private List<EACCAResult> doEACCA(PassportService ps, MRZInfo mrzInfo, DG14File dg14File, SODFile sodFile){
        if(dg14File ==null){
            throw new NullPointerException("dg14File is null");
        }

        if(sodFile ==null){
            throw new NullPointerException("sodFile is null");
        }

        //Chip Authentication
        List<EACCAResult> eaccaResults = new ArrayList<>();
        
        ChipAuthenticationInfo chipAuthenticationInfo=null;

        List<ChipAuthenticationPublicKeyInfo> chipAuthenticationPublicKeyInfos = new ArrayList<>();
        Collection<SecurityInfo> securityInfos = dg14File.getSecurityInfos();
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

        return eaccaResults;
    }

    private List<EACTAResult> doEACTA(PassportService ps, MRZInfo mrzInfo, CVCAFile cvcaFile, PACEResult paceResult, EACCAResult eaccaResult, List<KeyStore> cvcaKeyStores) throws IOException, CardServiceException, GeneralSecurityException, IllegalArgumentException, NullPointerException {
        if(cvcaFile ==null){
            throw new NullPointerException("CVCAFile is null");
        }

        if(eaccaResult ==null){
            throw new NullPointerException("EACCAResult is null");
        }


        List<EACTAResult> eactaResults = new ArrayList<>();
        CVCPrincipal[] possibleCVCAReferences = new CVCPrincipal[]{ cvcaFile.getCAReference(), cvcaFile.getAltCAReference() };

        //EAC
        for (CVCPrincipal caReference: possibleCVCAReferences) {
            EACCredentials eacCredentials = PassportNfcUtils.getEACCredentials(caReference, cvcaKeyStores);
            if (eacCredentials == null) { continue; }

            PrivateKey privateKey = eacCredentials.getPrivateKey();
            Certificate[] chain = eacCredentials.getChain();
            List<CardVerifiableCertificate> terminalCerts = new ArrayList<CardVerifiableCertificate>(chain.length);
            for (Certificate c: chain) { terminalCerts.add((CardVerifiableCertificate)c); }

            try{
                if(paceResult==null) {
                    EACTAResult eactaResult = ps.doEACTA(caReference, terminalCerts, privateKey, null, eaccaResult, mrzInfo.getDocumentNumber());
                    eactaResults.add(eactaResult);
                } else{
                    EACTAResult eactaResult = ps.doEACTA(caReference, terminalCerts, privateKey, null, eaccaResult, paceResult);
                    eactaResults.add(eactaResult);
                }
            } catch(CardServiceException cse) {
                cse.printStackTrace();
                /* NOTE: Failed? Too bad, try next public key. */
                continue;
            }
            break;
        }

        return eactaResults;
    }


    private COMFile getComFile(PassportService ps) throws CardServiceException, IOException {
        //COM FILE
        InputStream isComFile = null;
        try{
            isComFile= ps.getInputStream(PassportService.EF_COM);
            return (COMFile) LDSFileUtil.getLDSFile(PassportService.EF_COM, isComFile);
        }
        finally {
            if(isComFile!=null){
                isComFile.close();
                isComFile = null;
            }
        }
    }

    private SODFile getSodFile(PassportService ps) throws CardServiceException, IOException {
        //SOD FILE
        InputStream isSodFile = null;
        try{
            isSodFile= ps.getInputStream(PassportService.EF_SOD);
            return (SODFile) LDSFileUtil.getLDSFile(PassportService.EF_SOD, isSodFile);
        }
        finally {
            if(isSodFile!=null){
                isSodFile.close();
                isSodFile = null;
            }
        }
    }
    
    private DG1File getDG1File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG1 = null;
        try {
            isDG1 = ps.getInputStream(PassportService.EF_DG1);
            return (DG1File) LDSFileUtil.getLDSFile(PassportService.EF_DG1, isDG1);
        }finally {
            if(isDG1!=null){
                isDG1.close();
                isDG1 = null;
            }
        }
    }

    private DG2File getDG2File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG2 = null;
        try {
            isDG2 = ps.getInputStream(PassportService.EF_DG2);
            return (DG2File) LDSFileUtil.getLDSFile(PassportService.EF_DG2, isDG2);
        }finally {
            if(isDG2!=null){
                isDG2.close();
                isDG2 = null;
            }
        }
    }

    private DG3File getDG3File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG3 = null;
        try {
            isDG3 = ps.getInputStream(PassportService.EF_DG3);
            return (DG3File) LDSFileUtil.getLDSFile(PassportService.EF_DG3, isDG3);
        }finally {
            if(isDG3!=null){
                isDG3.close();
                isDG3 = null;
            }
        }
    }

    private DG5File getDG5File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG5 = null;
        try {
            isDG5 = ps.getInputStream(PassportService.EF_DG5);
            return (DG5File) LDSFileUtil.getLDSFile(PassportService.EF_DG5, isDG5);
        }finally {
            if(isDG5!=null){
                isDG5.close();
                isDG5 = null;
            }
        }
    }

    private DG7File getDG7File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG7 = null;
        try {
            isDG7 = ps.getInputStream(PassportService.EF_DG7);
            return (DG7File) LDSFileUtil.getLDSFile(PassportService.EF_DG7, isDG7);
        }finally {
            if(isDG7!=null){
                isDG7.close();
                isDG7 = null;
            }
        }
    }

    private DG11File getDG11File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG11 = null;
        try {
            isDG11 = ps.getInputStream(PassportService.EF_DG11);
            return (DG11File) LDSFileUtil.getLDSFile(PassportService.EF_DG11, isDG11);
        }finally {
            if(isDG11!=null){
                isDG11.close();
                isDG11 = null;
            }
        }
    }

    private DG12File getDG12File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG12 = null;
        try {
            isDG12 = ps.getInputStream(PassportService.EF_DG12);
            return (DG12File) LDSFileUtil.getLDSFile(PassportService.EF_DG12, isDG12);
        }finally {
            if(isDG12!=null){
                isDG12.close();
                isDG12 = null;
            }
        }
    }
    
    private DG14File getDG14File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG14 = null;
        try {
            isDG14 = ps.getInputStream(PassportService.EF_DG14);
            return (DG14File) LDSFileUtil.getLDSFile(PassportService.EF_DG14, isDG14);
        }finally {
            if(isDG14!=null){
                isDG14.close();
                isDG14 = null;
            }
        }
    }

    private DG15File getDG15File(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isDG15 = null;
        try {
            isDG15 = ps.getInputStream(PassportService.EF_DG15);
            return (DG15File) LDSFileUtil.getLDSFile(PassportService.EF_DG15, isDG15);
        }finally {
            if(isDG15!=null){
                isDG15.close();
                isDG15 = null;
            }
        }
    }

    private CVCAFile getCVCAFile(PassportService ps) throws CardServiceException, IOException {
        // Basic data
        InputStream isEF_CVCA = null;
        try {
            isEF_CVCA = ps.getInputStream(PassportService.EF_CVCA);
            return (CVCAFile) LDSFileUtil.getLDSFile(PassportService.EF_CVCA, isEF_CVCA);
        }finally {
            if(isEF_CVCA!=null){
                isEF_CVCA.close();
                isEF_CVCA = null;
            }
        }
    }

    private List<Integer> toDataGroupList(int[] tagList) {
        if (tagList == null) { return null; }
        List<Integer> dgNumberList = new ArrayList<Integer>(tagList.length);
        for (int tag: tagList) {
            try {
                int dgNumber = LDSFileUtil.lookupDataGroupNumberByTag(tag);
                dgNumberList.add(dgNumber);
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "Could not find DG number for tag: " + Integer.toHexString(tag));
                nfe.printStackTrace();
            }
        }
        return dgNumberList;
    }

}
