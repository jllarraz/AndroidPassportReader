package android.garda.ie.mylibrary.test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.x500.X500Principal;

import org.jmrtd.cert.CSCAMasterList;
import org.jmrtd.cert.KeyStoreCertStoreParameters;
import org.jmrtd.cert.PKDCertStoreParameters;
import org.jmrtd.cert.PKDMasterListCertStoreParameters;

/**
 * Provides lookup for certificates, keys, CRLs used in
 * document validation and access control for data groups.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
public class MRTDTrustStore {

    private static final Provider JMRTD_PROVIDER = JMRTDSecurityProvider.getInstance();

    private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

    private static final CertSelector SELF_SIGNED_X509_CERT_SELECTOR = new X509CertSelector() {
        public boolean match(Certificate cert) {
            if (!(cert instanceof X509Certificate)) { return false; }
            X509Certificate x509Cert = (X509Certificate)cert;
            X500Principal issuer = x509Cert.getIssuerX500Principal();
            X500Principal subject = x509Cert.getSubjectX500Principal();
            return (issuer == null && subject == null) || subject.equals(issuer);
        }

        public Object clone() { return this; }
    };

    private Set cscaAnchors;
    private List cscaStores;
    private List cvcaStores;

    /**
     * Constructs an instance.
     */
    public MRTDTrustStore() {
        this(new HashSet(), new ArrayList(), new ArrayList());
    }

    /**
     * Constructs an instance.
     *
     * @param cscaAnchors the root certificates for document validation
     * @param cscaStores the certificates used in document validation
     * @param cvcaStores the certificates used for access to EAC protected data groups
     */
    public MRTDTrustStore(Set cscaAnchors, List cscaStores, List cvcaStores) {
        super();
        this.cscaAnchors = cscaAnchors;
        this.cscaStores = cscaStores;
        this.cvcaStores = cvcaStores;
    }

    public void clear() {
        this.cscaAnchors = new HashSet();
        this.cscaStores = new ArrayList();
        this.cvcaStores = new ArrayList();
    }

    /**
     * Gets the root certificates for document validation.
     *
     * @return the cscaAnchors
     */
    public Set getCSCAAnchors() {
        return cscaAnchors;
    }
    /**
     * Gets the certificates used in document validation.
     *
     * @return the cscaStores
     */
    public List getCSCAStores() {
        return cscaStores;
    }
    /**
     * Gets the certificates used for access to EAC protected data groups.
     *
     * @return the cvcaStores
     */
    public List getCVCAStores() {
        return cvcaStores;
    }

    /**
     * Adds a root certificate for document validation.
     *
     * @param trustAnchor a trustAnchor
     */
    public void addCSCAAnchor(TrustAnchor trustAnchor) {
        cscaAnchors.add(trustAnchor);
    }

    /**
     * Adds root certificates for document validation.
     *
     * @param trustAnchors a collection of trustAnchors
     */
    public void addCSCAAnchors(Collection trustAnchors) {
        cscaAnchors.addAll(trustAnchors);
    }

    /**
     * Adds a certificate store for document validation based on a URI.
     *
     * @param uri the URI
     */
    public void addCSCAStore(URI uri) {
        if (uri == null) { LOGGER.severe("uri == null"); return; }
        String scheme = uri.getScheme();
        if (scheme == null) { LOGGER.severe("scheme == null, location = " + uri); return; }
        try {
            if (scheme.equalsIgnoreCase("ldap")) {
                addAsPKDStoreCSCACertStore(uri);
            } else {
                /* The scheme is probably "file" or "http"? Going to just open a connection and read the contents. */
                try {
                    /* Is it a key store file? */
                    LOGGER.info("Trying to open " + uri.toASCIIString() + " as keystore file");
                    addAsKeyStoreCSCACertStore(uri);
                } catch (Exception e1) {
                    try {
                        /* Is it a CSCA master list? */
                        LOGGER.info("Trying to open " + uri.toASCIIString() + " as CSCA as master list");
                        addAsCSCAMasterList(uri);
                    } catch (Exception e2) {
                        try {
                            /* Is it a single certificate file? */
                            LOGGER.info("Trying to open " + uri.toASCIIString() + " as certificate file");
                            addAsSingletonCSCACertStore(uri);
                        } catch (Exception e3) {
                            LOGGER.warning("Failed to open " + uri.toASCIIString() + " as a keystore, as a DER certificate file, and as a CSCA masterlist file");
//							e1.printStackTrace();
//							e2.printStackTrace();
//							e3.printStackTrace();
                        }
                    }
                }
            }
        } catch (GeneralSecurityException gse) {
            gse.printStackTrace();
        }
    }

    /**
     * Adds multiple certificate stores for document validation based on URIs.
     *
     * @param uris the URIs
     */
    public void addCSCAStores(List uris) {
        if (uris == null) { LOGGER.severe("uris == null"); return; }
        for (URI uri: uris) {
            addCSCAStore(uri);
        }
    }

    /**
     * Adds a key store for access to EAC protected data groups based on a URI.
     *
     * @param uri the URI
     */
    public void addCVCAStore(URI uri) {
        try {
            addAsCVCAKeyStore(uri);
        } catch (Exception e) {
            LOGGER.warning("Exception in addCVCAStore: " + e.getMessage());
        }
    }

    /**
     * Adds multiple key stores for access to EAC protected data groups based on URIs.
     *
     * @param uris the URIs
     */
    public void addCVCAStores(List uris) {
        for (URI uri: uris) {
            addCVCAStore(uri);
        }
    }

    /**
     * Adds a certificate store for document validation.
     *
     * @param certStore the certificate store
     */
    public void addCSCAStore(CertStore certStore) {
        cscaStores.add(certStore);
    }

    /**
     * Adds a key store for access to EAC protected data groups.
     *
     * @param keyStore the key store
     */
    public void addCVCAStore(KeyStore keyStore) {
        cvcaStores.add(keyStore);
    }

    /**
     * Removes a trust anchor for document validation.
     *
     * @param trustAnchor the trust anchor
     */
    public void removeCSCAAnchor(TrustAnchor trustAnchor) {
        cscaAnchors.remove(trustAnchor);
    }

    /**
     * Removes a certificate store for document validation.
     *
     * @param certStore the certificate store
     */
    public void removeCSCAStore(CertStore certStore) {
        cscaStores.remove(certStore);
    }

    /**
     * Removes a key store for access to EAC protected data groups.
     *
     * @param keyStore the key store
     */
    public void removeCVCAStore(KeyStore keyStore) {
        cvcaStores.remove(keyStore);
    }

    /* ONLY PRIVATE METHODS BELOW */

    private void addAsSingletonCSCACertStore(URI uri) throws MalformedURLException, IOException, CertificateException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertStoreException {
        URLConnection urlConnection = uri.toURL().openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", JMRTD_PROVIDER);
        X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(inputStream);
        inputStream.close();
        CertStoreParameters params = new CollectionCertStoreParameters(Collections.singleton(certificate));
        CertStore cscaStore = CertStore.getInstance("Collection", params);
        cscaStores.add(cscaStore);
        Collection rootCerts = cscaStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
        addCSCAAnchors(getAsAnchors(rootCerts));
    }

    /**
     * Adds the CVCA key store located at uri.
     *
     * @param uri a URI with a key store
     */
    private void addAsCVCAKeyStore(URI uri) {
        addCVCAStore(getKeyStore(uri));
    }

    private void addAsPKDStoreCSCACertStore(URI uri) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, CertStoreException {
        /* PKD store */
        String server = uri.getHost();
        int port = uri.getPort();
        CertStoreParameters params = port < 0 ? new PKDCertStoreParameters(server) : new PKDCertStoreParameters(server, port);
        CertStoreParameters cscaParams = port < 0 ? new PKDMasterListCertStoreParameters(server) : new PKDMasterListCertStoreParameters(server, port);
        CertStore certStore = CertStore.getInstance("PKD", params);
        if (certStore != null) { addCSCAStore(certStore); }
        CertStore cscaStore = CertStore.getInstance("PKD", cscaParams);
        if (cscaStore != null) { addCSCAStore(cscaStore); }
        Collection rootCerts = cscaStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
        addCSCAAnchors(getAsAnchors(rootCerts));
    }

    private void addAsKeyStoreCSCACertStore(URI uri) throws KeyStoreException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertStoreException {
        KeyStore keyStore = getKeyStore(uri);
        CertStoreParameters params = new KeyStoreCertStoreParameters(keyStore);
        CertStore certStore = CertStore.getInstance(keyStore.getType(), params);
        addCSCAStore(certStore);
        Collection rootCerts = certStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
        addCSCAAnchors(getAsAnchors(rootCerts));
    }

    /* FIXME: skip KeyStore, and add directly as CertStore. */
    private void addAsCSCAMasterList(URI uri) throws IOException, GeneralSecurityException {
        URLConnection urlConnecton = uri.toURL().openConnection();
        DataInputStream dataInputStream = new DataInputStream(urlConnecton.getInputStream());
        byte[] bytes = new byte[(int)urlConnecton.getContentLengthLong()];
        dataInputStream.readFully(bytes);
        CSCAMasterList cscaMasterList = new CSCAMasterList(bytes);
        List certificates = cscaMasterList.getCertificates();
        CertStoreParameters params = new CollectionCertStoreParameters(certificates);
        CertStore certStore = CertStore.getInstance("Collection", params);
        addCSCAStore(certStore);
        Collection rootCerts = certStore.getCertificates(SELF_SIGNED_X509_CERT_SELECTOR);
        addCSCAAnchors(getAsAnchors(rootCerts));
    }

    private KeyStore getKeyStore(URI uri) {
        /*
         * We have to try all store types, only Bouncy Castle Store (BKS)
         * knows about unnamed EC keys.
         */
        String[] storeTypes = new String[] { "JKS", "BKS", "PKCS12" };
        for(String storeType : storeTypes) {
            try {
                KeyStore keyStore = KeyStore.getInstance(storeType);
                URLConnection urlConnection = uri.toURL().openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                keyStore.load(inputStream, "".toCharArray());
                inputStream.close();
                return keyStore;
            } catch (Exception e) {
                // LOGGER.warning("Could not initialize CVCA key store with type " + storeType + ": " + e.getMessage());
                // e.printStackTrace();
                continue;
            }
        }
        throw new IllegalArgumentException("Not a supported keystore");
    }

    /**
     * Returns a set of trust anchors based on the X509 certificates in certificates.
     *
     * @param certificates a collection of X509 certificates
     *
     * @return a set of trust anchors
     */
    private static Set getAsAnchors(Collection certificates) {
        Set anchors = new HashSet(certificates.size());
        for (Certificate certificate: certificates) {
            if (certificate instanceof X509Certificate) {
                anchors.add(new TrustAnchor((X509Certificate)certificate, null));
            }
        }
        return anchors;
    }
}
