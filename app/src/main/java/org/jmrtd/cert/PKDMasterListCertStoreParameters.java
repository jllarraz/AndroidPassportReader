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

/**
 * Parameters for PKD backed certificate store, selecting certificates provided
 * in CSCA master lists.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public class PKDMasterListCertStoreParameters extends PKDCertStoreParameters {
	
	private static final String DEFAULT_BASE_DN = "dc=CSCAMasterList,dc=pkdDownload";
	
	public PKDMasterListCertStoreParameters() {
		super();
	}

	public PKDMasterListCertStoreParameters(String serverName) {
		this(serverName, DEFAULT_BASE_DN);
	}

	public PKDMasterListCertStoreParameters(String serverName, String baseDN) {
		super(serverName, baseDN);
	}

	public PKDMasterListCertStoreParameters(String serverName, int port) {
		this(serverName, port, DEFAULT_BASE_DN);
	}

	public PKDMasterListCertStoreParameters(String serverName, int port, String baseDN) {
		super(serverName, port, baseDN);
	}
}
