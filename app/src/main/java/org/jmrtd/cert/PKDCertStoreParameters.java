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

import java.security.cert.CertStoreParameters;

/**
 * Parameters for PKD backed certificate store.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public class PKDCertStoreParameters implements Cloneable, CertStoreParameters {
	
	private static final String DEFAULT_SERVER_NAME = "localhost";
	private static final int DEFAULT_PORT = 389;
	private static final String DEFAULT_BASE_DN = "dc=data,dc=pkdDownload";

	private String serverName;
	private int port;
	private String baseDN;	

	public PKDCertStoreParameters() {
		this(DEFAULT_SERVER_NAME, DEFAULT_PORT, DEFAULT_BASE_DN);
	}

	public PKDCertStoreParameters(String serverName) {
		this(serverName, DEFAULT_PORT, DEFAULT_BASE_DN);
	}

	public PKDCertStoreParameters(String serverName, String baseDN) {
		this(serverName, DEFAULT_PORT, baseDN);
	}

	public PKDCertStoreParameters(String serverName, int port) {
		this(serverName, port, DEFAULT_BASE_DN);
	}

	public PKDCertStoreParameters(String serverName, int port, String baseDN) {
		this.serverName = serverName;
		this.port = port;
		this.baseDN = baseDN;
	}

	/**
	 * @return the serverName
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the baseDN
	 */
	public String getBaseDN() {
		return baseDN;
	}

	/**
	 * Makes a copy of this object.
	 * 
	 * @return a copy of this object
	 */
	public Object clone() {
		return new PKDCertStoreParameters(serverName, port, baseDN);
	}

	public String toString() {
		return "PKDCertStoreParameters [" + serverName + ":" + port + "/" + baseDN + "]";
	}

	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (otherObj == this) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		PKDCertStoreParameters otherParams = (PKDCertStoreParameters)otherObj;
		return otherParams.serverName.equals(this.serverName)
		&& otherParams.port == this.port
		&& otherParams.baseDN.equals(this.baseDN);
	}

	public int hashCode() {
		return (serverName.hashCode() + port + baseDN.hashCode()) * 2 + 303;
	}
}
