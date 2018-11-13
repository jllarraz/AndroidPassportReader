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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactorySpi;
import java.util.Collection;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVOutputStream;


import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.exception.ConstructionException;
import org.ejbca.cvc.exception.ParseException;

/**
 * Card verifiable certificate factory.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 * 
 * @see CardVerifiableCertificate
 */
public class CVCertificateFactorySpi extends CertificateFactorySpi {

	private static final int CV_CERTIFICATE_TAG = 0x7F21;

	public Certificate engineGenerateCertificate(InputStream inputStream) throws CertificateException {
		try {
			/* Read certificate as byte[] */
			TLVInputStream tlvIn = new TLVInputStream(inputStream);
			int tag = tlvIn.readTag();
			if (tag != CV_CERTIFICATE_TAG) { throw new CertificateException("Expected CV_CERTIFICATE_TAG, found " + Integer.toHexString(tag)); }
			/* int length = */ tlvIn.readLength();
			byte[] value = tlvIn.readValue();
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TLVOutputStream tlvOut = new TLVOutputStream(out);
			tlvOut.writeTag(CV_CERTIFICATE_TAG);
			tlvOut.writeValue(value);
			tlvOut.close();
			CVCObject parsedObject = CertificateParser.parseCertificate(out.toByteArray());
			return new CardVerifiableCertificate((org.ejbca.cvc.CVCertificate)parsedObject);
		} catch (IOException ioe) {
			throw new CertificateException(ioe.getMessage());
		} catch (ConstructionException ce) {
			throw new CertificateException(ce.getMessage());
		} catch (ParseException pe) {
			throw new CertificateException(pe.getMessage());
		}
	}

	/**
	 * Not implemented.
	 * 
	 * @param inputStream input stream
	 */
	public CRL engineGenerateCRL(InputStream inputStream) throws CRLException {
		return null; // TODO
	}

	/**
	 * Not implemented.
	 * 
	 * @param inputStream input stream
	 */
	public Collection<? extends CRL> engineGenerateCRLs(InputStream inputStream) throws CRLException {
		return null; // TODO
	}

	public Collection<? extends Certificate> engineGenerateCertificates(InputStream in) throws CertificateException {
		return null; // TODO
	}
}
