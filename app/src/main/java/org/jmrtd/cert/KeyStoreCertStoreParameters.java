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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertStoreParameters;
import java.util.logging.Logger;

import org.jmrtd.JMRTDSecurityProvider;

/**
 * Parameters for key store backed certificate store.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public class KeyStoreCertStoreParameters implements Cloneable, CertStoreParameters {

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private static final String DEFAULT_ALGORITHM = "JKS";
	private static final char[] DEFAULT_PASSWORD = "".toCharArray();

	private KeyStore keyStore;

	public KeyStoreCertStoreParameters(URI uri) throws KeyStoreException {
		this(uri, DEFAULT_ALGORITHM, DEFAULT_PASSWORD);
	}

	public KeyStoreCertStoreParameters(URI uri, char[] password) throws KeyStoreException {
		this(uri, DEFAULT_ALGORITHM, password);
	}

	public KeyStoreCertStoreParameters(URI uri, String algorithm) throws KeyStoreException {
		this(uri, algorithm, DEFAULT_PASSWORD);
	}

	public KeyStoreCertStoreParameters(URI uri, String algorithm, char[] password) throws KeyStoreException {
		this(readKeyStore(uri, algorithm, password));
	}

	public KeyStoreCertStoreParameters(KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}

	/**
	 * Makes a shallow copy of this object as this
	 * class is immutable.
	 * 
	 * @return a shallow copy of this object
	 */
	public Object clone() {
		return new KeyStoreCertStoreParameters(keyStore);
	}

	private static KeyStore readKeyStore(URI location, String keyStoreType, char[] password) throws KeyStoreException {
		try {
			int n = JMRTDSecurityProvider.beginPreferBouncyCastleProvider();
			URLConnection uc = location.toURL().openConnection();
			InputStream inputStream = uc.getInputStream();
			KeyStore ks = null;
			ks = KeyStore.getInstance(keyStoreType);
			try {
				LOGGER.info("KeystoreCertStore will use provider for KeyStore: " + ks.getProvider().getClass().getCanonicalName());
				ks.load(inputStream, password);
			} catch (IOException ioe) {
				LOGGER.warning("Cannot read this file \"" + location + "\" as keystore");
				// ioe.printStackTrace();
			}
			inputStream.close();
			JMRTDSecurityProvider.endPreferBouncyCastleProvider(n);
			return ks;
		} catch (Exception e) {
			// e.printStackTrace();
			throw new KeyStoreException("Error getting keystore: " + e.getMessage());
		}
	}
}
