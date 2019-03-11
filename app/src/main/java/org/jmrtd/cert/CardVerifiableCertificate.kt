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

package org.jmrtd.cert

import net.sf.scuba.data.Country

import java.io.IOException
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.Provider
import java.security.PublicKey
import java.security.Security
import java.security.SignatureException
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Date


import org.ejbca.cvc.AccessRightEnum
import org.ejbca.cvc.AlgorithmUtil
import org.ejbca.cvc.AuthorizationRoleEnum
import org.ejbca.cvc.CAReferenceField
import org.ejbca.cvc.CVCertificateBody
import org.ejbca.cvc.HolderReferenceField
import org.ejbca.cvc.OIDField
import org.ejbca.cvc.ReferenceField
import org.ejbca.cvc.exception.ConstructionException

/**
 * Card verifiable certificates as specified in TR 03110.
 *
 * Just a wrapper around `org.ejbca.cvc.CVCertificate` by Keijo Kurkinen of EJBCA.org,
 * so that we can subclass `java.security.cert.Certificate`.
 *
 * We also hide some of the internal structure (no more calls to get the "body" just to get some
 * attributes).
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
class CardVerifiableCertificate : Certificate {

    /** The EJBCA CVC that we wrap.  */
    private var cvCertificate: org.ejbca.cvc.CVCertificate? = null

    @Transient
    private var rsaKeyFactory: KeyFactory? = null

    val sigAlgName: String?
        get() {
            try {
                val oid = cvCertificate!!.certificateBody.publicKey.objectIdentifier
                return AlgorithmUtil.getAlgorithmName(oid)
            } catch (nsfe: NoSuchFieldException) {
                return null
            }

        }

    val sigAlgOID: String?
        get() {
            try {
                val oid = cvCertificate!!.certificateBody.publicKey.objectIdentifier
                return oid.asText
            } catch (nsfe: NoSuchFieldException) {
                return null
            }

        }

    /**
     * The DER encoded certificate body.
     *
     * @return DER encoded certificate body
     *
     * @throws CertificateException on error
     * @throws IOException on error
     */
    val certBodyData: ByteArray
        @Throws(CertificateException::class, IOException::class)
        get() {
            try {
                return cvCertificate!!.certificateBody.derEncoded
            } catch (nsfe: NoSuchFieldException) {
                throw CertificateException(nsfe.message)
            }

        }

    /**
     * Returns 'Effective Date'.
     *
     * @return the effective date
     */
    val notBefore: Date
        @Throws(CertificateException::class)
        get() {
            try {
                return cvCertificate!!.certificateBody.validFrom
            } catch (nsfe: NoSuchFieldException) {
                throw CertificateException(nsfe.message)
            }

        }

    /**
     * Returns 'Expiration Date'.
     *
     * @return the expiration date
     */
    val notAfter: Date
        @Throws(CertificateException::class)
        get() {
            try {
                return cvCertificate!!.certificateBody.validTo
            } catch (nsfe: NoSuchFieldException) {
                throw CertificateException(nsfe.message)
            }

        }

    /**
     * Gets the authority reference.
     *
     * @return the authority reference
     *
     * @throws CertificateException if the authority reference field is not present
     */
    val authorityReference: CVCPrincipal
        @Throws(CertificateException::class)
        get() {
            try {
                val rf = cvCertificate!!.certificateBody.authorityReference
                val countryCode = rf.country.toUpperCase()
                val country = Country.getInstance(countryCode)
                return CVCPrincipal(country, rf.mnemonic, rf.sequence)
            } catch (nsfe: NoSuchFieldException) {
                throw CertificateException(nsfe.message)
            }

        }

    /**
     * Gets the holder reference.
     *
     * @return the holder reference
     *
     * @throws CertificateException if the authority reference field is not present
     */
    val holderReference: CVCPrincipal
        @Throws(CertificateException::class)
        get() {
            try {
                val rf = cvCertificate!!.certificateBody.holderReference
                return CVCPrincipal(Country.getInstance(rf.country.toUpperCase()), rf.mnemonic, rf.sequence)
            } catch (nsfe: NoSuchFieldException) {
                throw CertificateException(nsfe.message)
            }

        }

    /**
     * Gets the holder authorization template.
     *
     * @return the holder authorization template
     * @throws CertificateException
     */
    val authorizationTemplate: CVCAuthorizationTemplate
        @Throws(CertificateException::class)
        get() {
            try {
                val template = cvCertificate!!.certificateBody.authorizationTemplate
                return CVCAuthorizationTemplate(template)
            } catch (nsfe: NoSuchFieldException) {
                throw CertificateException(nsfe.message)
            }

        }

    /**
     * Returns the signature (just the value, without the `0x5F37` tag).
     * @return the signature bytes
     *
     * @throws CertificateException if certificate doesn't contain a signature
     */
    val signature: ByteArray
        @Throws(CertificateException::class)
        get() {
            try {
                return cvCertificate!!.signature
            } catch (nsfe: NoSuchFieldException) {
                throw CertificateException(nsfe.message)
            }

        }

    /**
     * Constructs a wrapper.
     *
     * @param cvCertificate the EJCBA CVC to wrap
     */
    constructor(cvCertificate: org.ejbca.cvc.CVCertificate) : super("CVC") {
        try {
            rsaKeyFactory = KeyFactory.getInstance("RSA")
        } catch (nsae: NoSuchAlgorithmException) {
            /* NOTE: never happens, RSA will be provided. */
            nsae.printStackTrace()
        }

        this.cvCertificate = cvCertificate
    }

    /*
	 * TODO: perhaps move this to factory class (CertificateFactory, CertificateBuilder, whatever).
	 * NOTE: algorithm should be one of"SHA224withECDSA", "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA",
	 * or similar with RSA.
	 *
	 */
    constructor(authorityReference: CVCPrincipal, holderReference: CVCPrincipal,
                publicKey: PublicKey,
                algorithm: String,
                notBefore: Date,
                notAfter: Date,
                role: CVCAuthorizationTemplate.Role,
                permission: CVCAuthorizationTemplate.Permission,
                signatureData: ByteArray) : super("CVC") {
        try {
            val authorityRef = CAReferenceField(authorityReference.country.toAlpha2Code(), authorityReference.mnemonic, authorityReference.seqNumber)
            val holderRef = HolderReferenceField(holderReference.country.toAlpha2Code(), holderReference.mnemonic, holderReference.seqNumber)
            val authRole = CVCAuthorizationTemplate.fromRole(role)
            val accessRight = CVCAuthorizationTemplate.fromPermission(permission)
            val body = CVCertificateBody(authorityRef, org.ejbca.cvc.KeyFactory.createInstance(publicKey, algorithm, authRole), holderRef, authRole, accessRight, notBefore, notAfter)
            this.cvCertificate = org.ejbca.cvc.CVCertificate(body)
            this.cvCertificate!!.signature = signatureData
            cvCertificate!!.tbs
        } catch (ce: ConstructionException) {
            throw IllegalArgumentException(ce.message)
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
    @Throws(CertificateEncodingException::class)
    override fun getEncoded(): ByteArray {
        try {
            return cvCertificate!!.derEncoded
        } catch (ioe: IOException) {
            throw CertificateEncodingException(ioe.message)
        }

    }

    /**
     * Gets the public key from this certificate.
     *
     * @return the public key.
     */
    override fun getPublicKey(): PublicKey? {
        try {
            val publicKey = cvCertificate!!.certificateBody.publicKey
            if (publicKey.algorithm == "RSA") { // TODO: something similar for EC / ECDSA?
                val rsaPublicKey = publicKey as RSAPublicKey
                try {
                    return rsaKeyFactory!!.generatePublic(RSAPublicKeySpec(rsaPublicKey.modulus, rsaPublicKey.publicExponent))
                } catch (gse: GeneralSecurityException) {
                    gse.printStackTrace()
                    return publicKey
                }

            }

            /* It's ECDSA... */
            return publicKey
        } catch (nsfe: NoSuchFieldException) {
            nsfe.printStackTrace()
            return null
        }

    }

    /**
     * Returns a string representation of this certificate.
     *
     * @return a string representation of this certificate.
     */
    override fun toString(): String {
        return cvCertificate!!.toString()
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
    @Throws(CertificateException::class, NoSuchAlgorithmException::class, InvalidKeyException::class, NoSuchProviderException::class, SignatureException::class)
    override fun verify(key: PublicKey) {
        val providers = Security.getProviders()
        var foundProvider = false
        for (provider in providers) {
            try {
                cvCertificate!!.verify(key, provider.name)
                foundProvider = true
                break
            } catch (nse: NoSuchAlgorithmException) {
                continue
            }

        }
        if (!foundProvider) {
            throw NoSuchAlgorithmException("Tried all security providers: None was able to provide this signature algorithm.")
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
    @Throws(CertificateException::class, NoSuchAlgorithmException::class, InvalidKeyException::class, NoSuchProviderException::class, SignatureException::class)
    override fun verify(key: PublicKey, provider: String) {
        cvCertificate!!.verify(key, provider)
    }

    override fun equals(otherObj: Any?): Boolean {
        if (otherObj == null) {
            return false
        }
        if (this === otherObj) {
            return true
        }
        return if (this.javaClass != otherObj.javaClass) {
            false
        } else this.cvCertificate == (otherObj as CardVerifiableCertificate).cvCertificate
    }

    override fun hashCode(): Int {
        return cvCertificate!!.hashCode() * 2 - 1030507011
    }

    companion object {

        private val serialVersionUID = -3585440601605666288L
    }
}