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

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.cert.CRL
import java.security.cert.CRLException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactorySpi

import net.sf.scuba.tlv.TLVInputStream
import net.sf.scuba.tlv.TLVOutputStream


import org.ejbca.cvc.CVCObject
import org.ejbca.cvc.CertificateParser
import org.ejbca.cvc.exception.ConstructionException
import org.ejbca.cvc.exception.ParseException

/**
 * Card verifiable certificate factory.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 *
 * @see CardVerifiableCertificate
 */
class CVCertificateFactorySpi : CertificateFactorySpi() {

    @Throws(CertificateException::class)
    override fun engineGenerateCertificate(inputStream: InputStream): Certificate {
        try {
            /* Read certificate as byte[] */
            val tlvIn = TLVInputStream(inputStream)
            val tag = tlvIn.readTag()
            if (tag != CV_CERTIFICATE_TAG) {
                throw CertificateException("Expected CV_CERTIFICATE_TAG, found " + Integer.toHexString(tag))
            }
            /* int length = */ tlvIn.readLength()
            val value = tlvIn.readValue()

            val out = ByteArrayOutputStream()
            val tlvOut = TLVOutputStream(out)
            tlvOut.writeTag(CV_CERTIFICATE_TAG)
            tlvOut.writeValue(value)
            tlvOut.close()
            val parsedObject = CertificateParser.parseCertificate(out.toByteArray())
            return CardVerifiableCertificate(parsedObject as org.ejbca.cvc.CVCertificate)
        } catch (ioe: IOException) {
            throw CertificateException(ioe.message)
        } catch (ce: ConstructionException) {
            throw CertificateException(ce.message)
        } catch (pe: ParseException) {
            throw CertificateException(pe.message)
        }

    }

    /**
     * Not implemented.
     *
     * @param inputStream input stream
     */
    @Throws(CRLException::class)
    override fun engineGenerateCRL(inputStream: InputStream): CRL? {
        return null // TODO
    }

    /**
     * Not implemented.
     *
     * @param inputStream input stream
     */
    @Throws(CRLException::class)
    override fun engineGenerateCRLs(inputStream: InputStream): Collection<CRL>? {
        return null // TODO
    }

    @Throws(CertificateException::class)
    override fun engineGenerateCertificates(`in`: InputStream): Collection<Certificate>? {
        return null // TODO
    }

    companion object {

        private val CV_CERTIFICATE_TAG = 0x7F21
    }
}
