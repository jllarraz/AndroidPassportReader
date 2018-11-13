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
 * Card verifiable certificate authorization template.
 *  
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
public class CVCAuthorizationTemplate {

	/**
	 * The issuing authority.
	 * 
	 * @author The JMRTD team (info@jmrtd.org)
	 *
	 * @version $Revision: $
	 */
	public enum Role {
		CVCA  (0xC0),
		DV_D  (0x80),
		DV_F  (0x40),
		IS    (0x00);

		private byte value;

		private Role(int value) {
			this.value = (byte)value;
		}

		/**
		 * Returns the value as a bitmap.
		 * 
		 * @return a bitmap
		 */
		public byte getValue(){
			return value;
		}
	}

	/**
	 * The authorization permission.
	 * 
	 * @author The JMRTD team (info@jmrtd.org)
	 *
	 * @version $Revision: $
	 */
	public enum Permission
	{
		READ_ACCESS_NONE        (0x00),
		READ_ACCESS_DG3         (0x01),
		READ_ACCESS_DG4         (0x02),
		READ_ACCESS_DG3_AND_DG4 (0x03);

		private byte value;

		private Permission(int value){
			this.value = (byte)value;
		}

		/**
		 * Whether this permission implies the other permission.
		 * 
		 * @param other some other permission
		 * 
		 * @return a boolean
		 */
		public boolean implies(Permission other) {
			switch (this) {
			case READ_ACCESS_NONE: return other == READ_ACCESS_NONE;
			case READ_ACCESS_DG3: return other == READ_ACCESS_DG3;
			case READ_ACCESS_DG4: return other == READ_ACCESS_DG4;
			case READ_ACCESS_DG3_AND_DG4: return other == READ_ACCESS_DG3 || other == READ_ACCESS_DG4 || other == READ_ACCESS_DG3_AND_DG4;
			}
			return false;
		}

		/**
		 * Returns the tag as a bitmap
		 * 
		 * @return a bitmap
		 */
		public byte getValue(){
			return value;
		}
	}

	private Role role;
	private Permission accessRight;

	/**
	 * Constructs an authorization template based on an EJBCA authorization template.
	 * 
	 * @param template the authZ template to wrap
	 */
	protected CVCAuthorizationTemplate(org.ejbca.cvc.CVCAuthorizationTemplate template) {
		try {
			switch(template.getAuthorizationField().getRole()) {
			case CVCA: this.role = Role.CVCA; break;
			case DV_D: this.role = Role.DV_D; break;
			case DV_F: this.role = Role.DV_F; break;
			case IS: this.role = Role.IS; break;
			}
			switch(template.getAuthorizationField().getAccessRight()) {
			case READ_ACCESS_NONE: this.accessRight = Permission.READ_ACCESS_NONE; break;
			case READ_ACCESS_DG3: this.accessRight = Permission.READ_ACCESS_DG3; break;
			case READ_ACCESS_DG4: this.accessRight = Permission.READ_ACCESS_DG4; break;
			case READ_ACCESS_DG3_AND_DG4: this.accessRight = Permission.READ_ACCESS_DG3_AND_DG4; break;
			}
		} catch (NoSuchFieldException nsfe) {
			throw new IllegalArgumentException("Error getting role from AuthZ template");
		}
	}

	/**
	 * Constructs an authorization template.
	 * 
	 * @param role the role
	 * @param accessRight the access rights
	 */
	public CVCAuthorizationTemplate(Role role, Permission accessRight) {
		this.role = role;
		this.accessRight = accessRight;
	}

	/**
	 * Gets the role.
	 * 
	 * @return the role
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * Gets the access rights.
	 * 
	 * @return the access rights
	 */
	public Permission getAccessRight() {
		return accessRight;
	}

	/**
	 * Gets a textual representation of this authorization template.
	 * 
	 * @return a textual representation of this authorization template
	 */
	public String toString() {
		return role.toString() + accessRight.toString();
	}

	/**
	 * Checks equality.
	 *
	 * @param otherObj the other object
	 * 
	 * @return whether the other object is equal to this object
	 */
	public boolean equals(Object otherObj) {
		if (otherObj == null) { return false; }
		if (otherObj == this) { return true; }
		if (!this.getClass().equals(otherObj.getClass())) { return false; }
		CVCAuthorizationTemplate otherTemplate = (CVCAuthorizationTemplate)otherObj;
		return this.role == otherTemplate.role && this.accessRight == otherTemplate.accessRight;
	}

	/**
	 * Gets a hash code of this object.
	 * 
	 * @return the hash code
	 */
	public int hashCode() {
		return 2 * role.value + 3 * accessRight.value + 61;
	}

	static org.ejbca.cvc.AccessRightEnum fromPermission(Permission thisPermission) {
		try{
			switch(thisPermission) {
			case READ_ACCESS_NONE: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_NONE;
			case READ_ACCESS_DG3: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3;
			case READ_ACCESS_DG4: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG4;
			case READ_ACCESS_DG3_AND_DG4: return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3_AND_DG4;
			}
		} catch (Exception e) {
		}
		throw new IllegalArgumentException("Error getting permission from AuthZ template");
	}

	static org.ejbca.cvc.AuthorizationRoleEnum fromRole(Role thisRole) {
		try {
			switch(thisRole) {
			case CVCA: return org.ejbca.cvc.AuthorizationRoleEnum.CVCA;
			case DV_D: return org.ejbca.cvc.AuthorizationRoleEnum.DV_D;
			case DV_F: return org.ejbca.cvc.AuthorizationRoleEnum.DV_F;
			case IS: return org.ejbca.cvc.AuthorizationRoleEnum.IS;
			}
		} catch (Exception e) {
		}
		throw new IllegalArgumentException("Error getting role from AuthZ template");
	}
}
