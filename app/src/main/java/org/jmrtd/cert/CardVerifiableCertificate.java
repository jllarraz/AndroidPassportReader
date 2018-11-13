/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2013  The JMRTD team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id:  $
 */

package org.jmrtd.cert;

import net.sf.scuba.data.Country;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;



import org.ejbca.cvc.AccessRightEnum;
import org.ejbca.cvc.AlgorithmUtil;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCertificateBody;
import org.ejbca.cvc.HolderReferenceField;
import org.ejbca.cvc.OIDField;
import org.ejbca.cvc.ReferenceField;
import org.ejbca.cvc.exception.ConstructionException;

/**
 * Card verifiable certificates as specified in TR 03110.
 * 
 * Just a wrapper around <code>org.ejbca.cvc.CVCertificate</code> by Keijo Kurkinen of EJBCA.org,
 * so that we can subclass <code>java.security.cert.Certificate</code>.
 * 
 * We also hide some of the internal structure (no more calls to get the "body" just to get some
 * attributes).
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class CardVerifiableCertificate extends Certificate {

	private static final long serialVersionUID = -3585440601605666288L;

	/** The EJBCA CVC that we wrap. */
	private org.ejbca.cvc.CVCertificate cvCertificate;

	private transient KeyFactory rsaKeyFactory;

	/**
	 * Constructs a wrapper.
	 * 
	 * @param cvCertificate the EJCBA CVC to wrap
	 */
	protected CardVerifiableCertificate(org.ejbca.cvc.CVCertificate cvCertificate) {
		super("CVC");
		try {
			rsaKeyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException nsae) {
			/* NOTE: never happens, RSA will be provided. */
			nsae.printStackTrace();
		}
		this.cvCertificate = cvCertificate;
	}

	/*
	 * TODO: perhaps move this to factory class (CertificateFactory, CertificateBuilder, whatever).
	 * NOTE: algorithm should be one of"SHA224withECDSA", "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA",
	 * or similar with RSA.
	 * 
	 */
	public CardVerifiableCertificate(CVCPrincipal authorityReference, CVCPrincipal holderReference,
			PublicKey publicKey,
			String algorithm,
			Date notBefore,
			Date notAfter,
			CVCAuthorizationTemplate.Role role,
			CVCAuthorizationTemplate.Permission permission,
			byte[] signatureData) {
		super("CVC");
		try {
			CAReferenceField authorityRef = new CAReferenceField(authorityReference.getCountry().toAlpha2Code(), authorityReference.getMnemonic(), authorityReference.getSeqNumber());
			HolderReferenceField  holderRef = new HolderReferenceField(holderReference.getCountry().toAlpha2Code(), holderReference.getMnemonic(), holderReference.getSeqNumber());
			AuthorizationRoleEnum authRole = CVCAuthorizationTemplate.fromRole(role);
			AccessRightEnum accessRight = CVCAuthorizationTemplate.fromPermission(permission);
			CVCertificateBody body = new CVCertificateBody(authorityRef, org.ejbca.cvc.KeyFactory.createInstance(publicKey, algorithm, authRole), holderRef, authRole, accessRight, notBefore, notAfter);
			this.cvCertificate = new org.ejbca.cvc.CVCertificate(body);
			this.cvCertificate.setSignature(signatureData);
			cvCertificate.getTBS();
		} catch(ConstructionException ce) {
			throw new IllegalArgumentException(ce.getMessage());
		}
	}

	public String getSigAlgName() {
		try {
			OIDField oid = cvCertificate.getCertificateBody().getPublicKey().getObjectIdentifier();			
			String algorithm = AlgorithmUtil.getAlgorithmName(oid);
			return algorithm;
		} catch (NoSuchFieldException nsfe) {
			return null;
		}
	}

	public String getSigAlgOID() {
		try {
			OIDField oid = cvCertificate.getCertificateBody().getPublicKey().getObjectIdentifier();
			return oid.getAsText();
		} catch (NoSuchFieldException nsfe) {
			return null;
		}
	}

	/**
	 * Returns the encoded form of this certificate. It is
	 * assumed that each certificate type would have only a single
	 * form of encoding; for example, X.509 certificates would
	 * be encoded as ASN.1 DER.
	 *
	 * @return the encoded form of this certificate
	 *
	 * @exception CertificateEncodingException if an encoding error occurs.
	 */
	public byte[] getEncoded() throws CertificateEncodingException {
		try {
			return cvCertificate.getDEREncoded();
		} catch (IOException ioe) {
			throw new CertificateEncodingException(ioe.getMessage());
		}
	}

	/**
	 * Gets the public key from this certificate.
	 *
	 * @return the public key.
	 */
	public PublicKey getPublicKey() {
		try {
			org.ejbca.cvc.CVCPublicKey publicKey = cvCertificate.getCertificateBody().getPublicKey();
			if (publicKey.getAlgorithm().equals("RSA")) { // TODO: something similar for EC / ECDSA?
				RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
				try {
					return rsaKeyFactory.generatePublic(new RSAPublicKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent()));
				} catch (GeneralSecurityException gse) {
					gse.printStackTrace();
					return publicKey;
				}
			}

			/* It's ECDSA... */
			return publicKey;
		} catch (NoSuchFieldException nsfe) {
			nsfe.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a string representation of this certificate.
	 *
	 * @return a string representation of this certificate.
	 */
	public String toString() {
		return cvCertificate.toString();
	}

	/**
	 * Verifies that this certificate was signed using the
	 * private key that corresponds to the specified public key.
	 *
	 * @param key the PublicKey used to carry out the verification.
	 *
	 * @exception NoSuchAlgorithmException on unsupported signature
	 * algorithms.
	 * @exception InvalidKeyException on incorrect key.
	 * @exception NoSuchProviderException if there's no default provider.
	 * @exception SignatureException on signature errors.
	 * @exception CertificateException on encoding errors.
	 */
	public void verify(PublicKey key) throws CertificateException,
	NoSuchAlgorithmException, InvalidKeyException,
	NoSuchProviderException, SignatureException {
		Provider[] providers = Security.getProviders();
		boolean foundProvider = false;
		for (Provider provider: providers) {
			try {
				cvCertificate.verify(key, provider.getName());
				foundProvider = true;
				break;
			} catch (NoSuchAlgorithmException nse) {
				continue;
			}
		}
		if (!foundProvider) {
			throw new NoSuchAlgorithmException("Tried all security providers: None was able to provide this signature algorithm.");
		}
	}


	/**
	 * Verifies that this certificate was signed using the
	 * private key that corresponds to the specified public key.
	 * This method uses the signature verification engine
	 * supplied by the specified provider.
	 *
	 * @param key the PublicKey used to carry out the verification.
	 * @param provider the name of the signature provider.
	 *
	 * @exception NoSuchAlgorithmException on unsupported signature algorithms.
	 * @exception InvalidKeyException on incorrect key.
	 * @exception NoSuchProviderException on incorrect provider.
	 * @exception SignatureException on signature errors.
	 * @exception CertificateException on encoding errors.
	 */
	public void verify(PublicKey key, String provider)
			throws CertificateException, NoSuchAlgorithmException,
			InvalidKeyException, NoSuchProviderException, SignatureException {
		cvCertificate.verify(key, provider);
	}

	/**
	 * The DER encoded certificate body.
	 * 
	 * @return DER encoded certificate body
	 * 
	 * @throws CertificateException on error
	 * @throws IOException on error
	 */
	public byte[] getCertBodyData() throws CertificateException, IOException {
		try {
			return cvCertificate.getCertificateBody().getDEREncoded();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Returns 'Effective Date'.
	 * 
	 * @return the effective date
	 */
	public Date getNotBefore() throws CertificateException {
		try {
			return cvCertificate.getCertificateBody().getValidFrom();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Returns 'Expiration Date'.
	 * 
	 * @return the expiration date
	 */
	public Date getNotAfter() throws CertificateException {
		try {
			return cvCertificate.getCertificateBody().getValidTo();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Gets the authority reference.
	 * 
	 * @return the authority reference
	 * 
	 * @throws CertificateException if the authority reference field is not present
	 */
	public CVCPrincipal getAuthorityReference() throws CertificateException {
		try  {
			ReferenceField rf = cvCertificate.getCertificateBody().getAuthorityReference();
			final String countryCode = rf.getCountry().toUpperCase();
			Country country = Country.getInstance(countryCode);
			return new CVCPrincipal(country, rf.getMnemonic(), rf.getSequence());
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Gets the holder reference.
	 * 
	 * @return the holder reference
	 * 
	 * @throws CertificateException if the authority reference field is not present
	 */
	public CVCPrincipal getHolderReference() throws CertificateException {
		try  {
			ReferenceField rf = cvCertificate.getCertificateBody().getHolderReference();
			return new CVCPrincipal(Country.getInstance(rf.getCountry().toUpperCase()), rf.getMnemonic(), rf.getSequence());
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Gets the holder authorization template.
	 * 
	 * @return the holder authorization template
	 * @throws CertificateException
	 */
	public CVCAuthorizationTemplate getAuthorizationTemplate() throws CertificateException {
		try {
			org.ejbca.cvc.CVCAuthorizationTemplate template = cvCertificate.getCertificateBody().getAuthorizationTemplate();
			return new CVCAuthorizationTemplate(template);
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	/**
	 * Returns the signature (just the value, without the <code>0x5F37</code> tag).
	 * @return the signature bytes
	 * 
	 * @throws CertificateException if certificate doesn't contain a signature
	 */
	public byte[] getSignature() throws CertificateException {
		try {
			return cvCertificate.getSignature();
		} catch (NoSuchFieldException nsfe) {
			throw new CertificateException(nsfe.getMessage());
		}
	}

	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (this == otherObj) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		return this.cvCertificate.equals(((CardVerifiableCertificate)otherObj).cvCertificate);
	}

	public int hashCode() {
		return cvCertificate.hashCode() * 2 - 1030507011;
	}
}