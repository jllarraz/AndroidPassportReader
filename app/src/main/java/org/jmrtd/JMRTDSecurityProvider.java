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

package org.jmrtd;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Security provider for JMRTD specific implementations.
 * Main motivation is to make JMRTD less dependent on the BouncyCastle provider.
 * Provides:
 * <ul>
 *    <li>{@link java.security.cert.CertificateFactory} &quot;CVC&quot;
 *    	  (a factory for {@link org.jmrtd.cert.CardVerifiableCertificate} instances)
 *    </li>
 *    <li>{@link java.security.cert.CertStore} &quot;PKD&quot;
 *       (LDAP based <code>CertStore</code>,
 *       where the directory contains CSCA and document signer certificates)
 *    </li>
 *    <li>{@link java.security.cert.CertStore} &quot;JKS&quot;
 *       (<code>KeyStore</code> based <code>CertStore</code>,
 *       where the JKS formatted <code>KeyStore</code> contains CSCA certificates)
 *    </li>
 *    <li>{@link java.security.cert.CertStore} &quot;PKCS12&quot;
 *       (<code>KeyStore</code> based <code>CertStore</code>,
 *       where the PKCS#12 formatted <code>KeyStore</code> contains CSCA certificates)
 *    </li>
 * </ul>
 *
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 */
public class JMRTDSecurityProvider extends Provider {

	private static final long serialVersionUID = -2881416441551680704L;

	private static final Logger LOGGER = Logger.getLogger("org.jmrtd");

	private static final String
	SUN_PROVIDER_CLASS_NAME = "sun.security.provider.Sun",
	BC_PROVIDER_CLASS_NAME = "org.bouncycastle.jce.provider.BouncyCastleProvider",
	SC_PROVIDER_CLASS_NAME = "org.spongycastle.jce.provider.BouncyCastleProvider";

//	private static final Provider SUN_PROVIDER = null; // getProviderOrNull(SUN_PROVIDER_CLASS_NAME);
	private static final Provider BC_PROVIDER =
			 new org.bouncycastle.jce.provider.BouncyCastleProvider();
//			getProviderOrNull(BC_PROVIDER_CLASS_NAME);
	private static final Provider SC_PROVIDER =
			 new org.bouncycastle.jce.provider.BouncyCastleProvider();
//			getProviderOrNull(SC_PROVIDER_CLASS_NAME);
	private static final Provider JMRTD_PROVIDER = new JMRTDSecurityProvider();

	static {
		Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
		/*
				if (BC_PROVIDER != null) { Security.insertProviderAt(BC_PROVIDER, 1); }
				if (SC_PROVIDER != null) { Security.insertProviderAt(SC_PROVIDER, 2); }
				if (JMRTD_PROVIDER != null) { Security.insertProviderAt(JMRTD_PROVIDER, 3); }*/
	}

	private JMRTDSecurityProvider() {
		super("JMRTD", 0.1, "JMRTD Security Provider");
		put("CertificateFactory.CVC", "org.jmrtd.cert.CVCertificateFactorySpi");
		put("CertStore.PKD", "org.jmrtd.cert.PKDCertStoreSpi");
		put("CertStore.JKS", "org.jmrtd.cert.KeyStoreCertStoreSpi");
		put("CertStore.BKS", "org.jmrtd.cert.KeyStoreCertStoreSpi");
		put("CertStore.PKCS12", "org.jmrtd.cert.KeyStoreCertStoreSpi");

		if (BC_PROVIDER != null) {
			/* Replicate BC algorithms... */

			/* FIXME: this won't work, our provider is not signed! */
			//			replicateFromProvider("Cipher", "DESede/CBC/NoPadding", getBouncyCastleProvider());
			//			replicateFromProvider("Cipher", "RSA/ECB/PKCS1Padding", getBouncyCastleProvider());
			//			replicateFromProvider("Cipher", "RSA/NONE/NoPadding", getBouncyCastleProvider());
			//			replicateFromProvider("KeyFactory", "RSA", getBouncyCastleProvider());
			//			replicateFromProvider("KeyFactory", "DH", getBouncyCastleProvider());
			//			replicateFromProvider("Mac", "ISO9797ALG3MAC", getBouncyCastleProvider());
			//			replicateFromProvider("Mac", "ISO9797ALG3WITHISO7816-4PADDING", getBouncyCastleProvider());
			//			replicateFromProvider("SecretKeyFactory", "DESede", getBouncyCastleProvider());

			/* But these work fine. */
			replicateFromProvider("CertificateFactory", "X.509", getBouncyCastleProvider());
			replicateFromProvider("CertStore", "Collection", getBouncyCastleProvider());
//			replicateFromProvider("KeyStore", "JKS", SUN_PROVIDER);
			replicateFromProvider("MessageDigest", "SHA1", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA1withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "MD2withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "MD4withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "MD5withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA1withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA1withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA256withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA256withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA384withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA384withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA512withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA512withRSA/ISO9796-2", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA224withRSA", getBouncyCastleProvider());
			replicateFromProvider("Signature", "SHA224withRSA/ISO9796-2", getBouncyCastleProvider());

            replicateFromProvider("Signature", "SHA256withRSA/PSS", getBouncyCastleProvider());


            /* Testing 0.4.7 -- MO */
			//			replicateFromProvider("KeyStore", "UBER", getBouncyCastleProvider());
			//			replicateFromProvider("KeyPairGenerator", "ECDHC", getBouncyCastleProvider());
			//			replicateFromProvider("KeyPairGenerator", "ECDSA", getBouncyCastleProvider());
			//			replicateFromProvider("X509StreamParser", "CERTIFICATE", getBouncyCastleProvider());

			put("Alg.Alias.Mac.ISO9797Alg3Mac", "ISO9797ALG3MAC");
			put("Alg.Alias.CertificateFactory.X509", "X.509");
		}
	}

	private void replicateFromProvider(String serviceName, String algorithmName, Provider provider) {
		String name = serviceName + "." + algorithmName;
		Object service = provider.get(name);
		if (service != null) {
			put(name, service);
		}
	}

	public static Provider getInstance() {
		return JMRTD_PROVIDER;
	}

	/**
	 * Temporarily puts the BC provider on number one in the list of
	 * providers, until caller calls {@link #endPreferBouncyCastleProvider(int)}.
	 * 
	 * @return the index of BC, if it was present, in the list of providers
	 * 
	 * @see #endPreferBouncyCastleProvider(int)
	 */
	public static int beginPreferBouncyCastleProvider() {
		Provider bcProvider = getBouncyCastleProvider();
		if (bcProvider == null) { return -1; }
		Provider[] providers = Security.getProviders();
		for (int i = 0; i < providers.length; i++) {
			Provider provider = providers[i];
			if (bcProvider.getClass().getCanonicalName().equals(provider.getClass().getCanonicalName())) {
				Security.removeProvider(provider.getName());
				Security.insertProviderAt(bcProvider, 1);
				return i + 1;
			}
		}
		return -1;
	}
	
	/**
	 * Removes the BC provider from the number one position and puts it back
	 * at its original position, after a call to {@link #beginPreferBouncyCastleProvider()}.
	 * 
	 * @param i the original index of the BC provider
	 * 
	 * @see #beginPreferBouncyCastleProvider()
	 */
	public static void endPreferBouncyCastleProvider(int i) {
		Provider bcProvider = getBouncyCastleProvider();
		Security.removeProvider(bcProvider.getName());
		if (i > 0) {
			Security.insertProviderAt(bcProvider, i);
		}
	}

	/**
	 * Gets the BC provider, if present.
	 * 
	 * @return the BC provider, the SC provider, or <code>null</code>
	 */
	public static Provider getBouncyCastleProvider() {
		if (BC_PROVIDER != null) { return BC_PROVIDER; }
		if (SC_PROVIDER != null) { return SC_PROVIDER; }
		LOGGER.severe("No Bouncy or Spongy provider");
		return null;
	}

	/**
	 * Gets the SC provider, if present.
	 * 
	 * @return the SC provider, the BC provider, or <code>null</code>
	 */
	public static Provider getSpongyCastleProvider() {
		if (SC_PROVIDER != null) { return SC_PROVIDER; }
		if (BC_PROVIDER != null) { return BC_PROVIDER; }
		LOGGER.severe("No Bouncy or Spongy provider");
		return null;
	}

	private static Provider getProvider(String serviceName, String algorithmName) {
		List<Provider> providers = getProviders(serviceName, algorithmName);
		if (providers != null && providers.size() > 0) {
			return providers.get(0);
		}
		return null;
	}

	private static List<Provider> getProviders(String serviceName, String algorithmName) {
		if (Security.getAlgorithms(serviceName).contains(algorithmName)) {
			Provider[] providers = Security.getProviders(serviceName + "." + algorithmName);
			return new ArrayList<Provider>(Arrays.asList(providers));
		}
		if (BC_PROVIDER != null && BC_PROVIDER.getService(serviceName, algorithmName) != null) {
			return new ArrayList<Provider>(Collections.singletonList(BC_PROVIDER));
		}
		if (SC_PROVIDER != null && SC_PROVIDER.getService(serviceName, algorithmName) != null) {
			return new ArrayList<Provider>(Collections.singletonList(SC_PROVIDER));
		}
		if (JMRTD_PROVIDER != null && JMRTD_PROVIDER.getService(serviceName, algorithmName) != null) {
			return new ArrayList<Provider>(Collections.singletonList(JMRTD_PROVIDER));
		}
		return null;
	}
}
