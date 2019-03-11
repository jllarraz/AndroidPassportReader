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

import org.ejbca.cvc.AccessRightEnum
import org.ejbca.cvc.AuthorizationRoleEnum

/**
 * Card verifiable certificate authorization template.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: $
 */
class CVCAuthorizationTemplate {

    /**
     * Gets the role.
     *
     * @return the role
     */
    var role: Role? = null
        private set
    /**
     * Gets the access rights.
     *
     * @return the access rights
     */
    var accessRight: Permission? = null
        private set

    /**
     * The issuing authority.
     *
     * @author The JMRTD team (info@jmrtd.org)
     *
     * @version $Revision: $
     */
    enum class Role private constructor(value: Int) {
        CVCA(0xC0),
        DV_D(0x80),
        DV_F(0x40),
        IS(0x00);

        /**
         * Returns the value as a bitmap.
         *
         * @return a bitmap
         */
        val value: Byte

        init {
            this.value = value.toByte()
        }
    }

    /**
     * The authorization permission.
     *
     * @author The JMRTD team (info@jmrtd.org)
     *
     * @version $Revision: $
     */
    enum class Permission private constructor(value: Int) {
        READ_ACCESS_NONE(0x00),
        READ_ACCESS_DG3(0x01),
        READ_ACCESS_DG4(0x02),
        READ_ACCESS_DG3_AND_DG4(0x03);

        /**
         * Returns the tag as a bitmap
         *
         * @return a bitmap
         */
        val value: Byte

        init {
            this.value = value.toByte()
        }

        /**
         * Whether this permission implies the other permission.
         *
         * @param other some other permission
         *
         * @return a boolean
         */
        fun implies(other: Permission): Boolean {
            when (this) {
                READ_ACCESS_NONE -> return other == READ_ACCESS_NONE
                READ_ACCESS_DG3 -> return other == READ_ACCESS_DG3
                READ_ACCESS_DG4 -> return other == READ_ACCESS_DG4
                READ_ACCESS_DG3_AND_DG4 -> return other == READ_ACCESS_DG3 || other == READ_ACCESS_DG4 || other == READ_ACCESS_DG3_AND_DG4
            }
            return false
        }
    }

    /**
     * Constructs an authorization template based on an EJBCA authorization template.
     *
     * @param template the authZ template to wrap
     */
    constructor(template: org.ejbca.cvc.CVCAuthorizationTemplate) {
        try {
            when (template.authorizationField.role) {
                AuthorizationRoleEnum.CVCA -> this.role = Role.CVCA
                AuthorizationRoleEnum.DV_D -> this.role = Role.DV_D
                AuthorizationRoleEnum.DV_F -> this.role = Role.DV_F
                AuthorizationRoleEnum.IS -> this.role = Role.IS
            }
            when (template.authorizationField.accessRight) {
                AccessRightEnum.READ_ACCESS_NONE -> this.accessRight = Permission.READ_ACCESS_NONE
                AccessRightEnum.READ_ACCESS_DG3 -> this.accessRight = Permission.READ_ACCESS_DG3
                AccessRightEnum.READ_ACCESS_DG4 -> this.accessRight = Permission.READ_ACCESS_DG4
                AccessRightEnum.READ_ACCESS_DG3_AND_DG4 -> this.accessRight = Permission.READ_ACCESS_DG3_AND_DG4
            }
        } catch (nsfe: NoSuchFieldException) {
            throw IllegalArgumentException("Error getting role from AuthZ template")
        }

    }

    /**
     * Constructs an authorization template.
     *
     * @param role the role
     * @param accessRight the access rights
     */
    constructor(role: Role, accessRight: Permission) {
        this.role = role
        this.accessRight = accessRight
    }

    /**
     * Gets a textual representation of this authorization template.
     *
     * @return a textual representation of this authorization template
     */
    override fun toString(): String {
        return role!!.toString() + accessRight!!.toString()
    }

    /**
     * Checks equality.
     *
     * @param otherObj the other object
     *
     * @return whether the other object is equal to this object
     */
    override fun equals(otherObj: Any?): Boolean {
        if (otherObj == null) {
            return false
        }
        if (otherObj === this) {
            return true
        }
        if (this.javaClass != otherObj.javaClass) {
            return false
        }
        val otherTemplate = otherObj as CVCAuthorizationTemplate?
        return this.role == otherTemplate!!.role && this.accessRight == otherTemplate.accessRight
    }

    /**
     * Gets a hash code of this object.
     *
     * @return the hash code
     */
    override fun hashCode(): Int {
        return 2 * role!!.value + 3 * accessRight!!.value + 61
    }

    companion object {

        internal fun fromPermission(thisPermission: Permission): org.ejbca.cvc.AccessRightEnum {
            try {
                when (thisPermission) {
                    CVCAuthorizationTemplate.Permission.READ_ACCESS_NONE -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_NONE
                    CVCAuthorizationTemplate.Permission.READ_ACCESS_DG3 -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3
                    CVCAuthorizationTemplate.Permission.READ_ACCESS_DG4 -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG4
                    CVCAuthorizationTemplate.Permission.READ_ACCESS_DG3_AND_DG4 -> return org.ejbca.cvc.AccessRightEnum.READ_ACCESS_DG3_AND_DG4
                }
            } catch (e: Exception) {
            }

            throw IllegalArgumentException("Error getting permission from AuthZ template")
        }

        internal fun fromRole(thisRole: Role): org.ejbca.cvc.AuthorizationRoleEnum {
            try {
                when (thisRole) {
                    CVCAuthorizationTemplate.Role.CVCA -> return org.ejbca.cvc.AuthorizationRoleEnum.CVCA
                    CVCAuthorizationTemplate.Role.DV_D -> return org.ejbca.cvc.AuthorizationRoleEnum.DV_D
                    CVCAuthorizationTemplate.Role.DV_F -> return org.ejbca.cvc.AuthorizationRoleEnum.DV_F
                    CVCAuthorizationTemplate.Role.IS -> return org.ejbca.cvc.AuthorizationRoleEnum.IS
                }
            } catch (e: Exception) {
            }

            throw IllegalArgumentException("Error getting role from AuthZ template")
        }
    }
}
