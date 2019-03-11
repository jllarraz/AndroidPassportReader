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

import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SignatureException
import java.util.Date

import org.ejbca.cvc.CAReferenceField
import org.ejbca.cvc.HolderReferenceField
import org.ejbca.cvc.exception.ConstructionException
import org.jmrtd.cert.CVCAuthorizationTemplate.Permission
import org.jmrtd.cert.CVCAuthorizationTemplate.Role

/**
 * Card verifiable certificate builder.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
object CVCertificateBuilder {

    /**
     * Card verifiable certificate factory method.
     *
     * @param publicKey the public key
     * @param signerKey private key
     * @param algorithmName algorithm name
     * @param caRef CA principal
     * @param holderRef holder principal
     * @param authZTemplate authorization template
     * @param validFrom valid from date
     * @param validTo valid to date
     * @param provider provider name
     *
     * @return a card verifiable certificate
     *
     * @throws IOException on error
     * @throws NoSuchAlgorithmException on unknown algorithm
     * @throws NoSuchProviderException  on unknown provider
     * @throws InvalidKeyException on invalid key
     * @throws SignatureException on error creating signature
     * @throws ConstructionException on error constructing the certificate
     */
    @Throws(IOException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class, InvalidKeyException::class, SignatureException::class, ConstructionException::class)
    fun createCertificate(publicKey: PublicKey,
                          signerKey: PrivateKey, algorithmName: String, caRef: CVCPrincipal,
                          holderRef: CVCPrincipal, authZTemplate: CVCAuthorizationTemplate, validFrom: Date, validTo: Date,
                          provider: String): CardVerifiableCertificate {
        return CardVerifiableCertificate(org.ejbca.cvc.CertificateGenerator
                .createCertificate(publicKey, signerKey, algorithmName,
                        CAReferenceField(caRef.country.toAlpha2Code(),
                                caRef.mnemonic, caRef.seqNumber),
                        HolderReferenceField(holderRef.country
                                .toAlpha2Code(), holderRef.mnemonic,
                                holderRef.seqNumber), getRole(authZTemplate.role!!), getAccessRight(authZTemplate.accessRight!!),
                        validFrom, validTo, provider))
    }

    private fun getRole(role: Role): org.ejbca.cvc.AuthorizationRoleEnum {
        when (role) {
            CVCAuthorizationTemplate.Role.CVCA -> return org.ejbca.cvc.AuthorizationRoleEnum.CVCA
            CVCAuthorizationTemplate.Role.DV_D -> return org.ejbca.cvc.AuthorizationRoleEnum.DV_D
            CVCAuthorizationTemplate.Role.DV_F -> return org.ejbca.cvc.AuthorizationRoleEnum.DV_F
            CVCAuthorizationTemplate.Role.IS -> return org.ejbca.cvc.AuthorizationRoleEnum.IS
        }
        throw NumberFormatException("Cannot decode role $role")
    }

    private fun getAccessRight(accessRight: Permission): org.ejbca.cvc.AccessRightEnum {
        when (accessRight) {
            CVCAuthorizationTemplate.Permission.READ_ACCESS_NONE -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_NONE
            CVCAuthorizationTemplate.Permission.READ_ACCESS_DG3 -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3
            CVCAuthorizationTemplate.Permission.READ_ACCESS_DG4 -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG4
            CVCAuthorizationTemplate.Permission.READ_ACCESS_DG3_AND_DG4 -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3_AND_DG4
        }
        throw NumberFormatException("Cannot decode access right $accessRight")
    }
}
