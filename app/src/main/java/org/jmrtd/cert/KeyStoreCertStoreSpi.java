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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CRL;
import java.security.cert.CRLSelector;
import java.security.cert.CertSelector;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertStoreSpi;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * Certificate store backed by key store.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class KeyStoreCertStoreSpi extends CertStoreSpi {

	private KeyStore keyStore;

	public KeyStoreCertStoreSpi(CertStoreParameters params) throws InvalidAlgorithmParameterException {
		super(params);
		keyStore = ((KeyStoreCertStoreParameters)params).getKeyStore();
	}

	public Collection<? extends Certificate> engineGetCertificates(CertSelector selector) throws CertStoreException {
		try {
			List<Certificate> certificates = new ArrayList<Certificate>(keyStore.size());
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = (String)aliases.nextElement();
				if (keyStore.isCertificateEntry(alias)) {
					Certificate certificate = keyStore.getCertificate(alias);
					if (selector.match(certificate)) {
						certificates.add(certificate);
					}
				}
			}
			return certificates;
		} catch (KeyStoreException kse) {
			throw new CertStoreException(kse.getMessage());
		}
	}

	public Collection<? extends CRL> engineGetCRLs(CRLSelector selector) throws CertStoreException {
		List<CRL> result = new ArrayList<CRL>(0);
		return result;
	}
}
