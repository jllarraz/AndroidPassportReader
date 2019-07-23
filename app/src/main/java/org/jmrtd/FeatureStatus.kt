package org.jmrtd

import android.os.Parcel
import android.os.Parcelable

/**
 * Security features of this identity document.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
class FeatureStatus : Parcelable {

    private var hasSAC: Verdict? = null
    private var hasBAC: Verdict? = null
    private var hasAA: Verdict? = null
    private var hasEAC: Verdict? = null
    private var hasCA: Verdict? = null

    /**
     * Outcome of a feature presence check.
     *
     * @author The JMRTD team (info@jmrtd.org)
     *
     * @version $Revision: 1559 $
     */
    enum class Verdict {
        UNKNOWN, /* Presence unknown */
        PRESENT, /* Present */
        NOT_PRESENT
        /* Not present */
    }

    constructor() {
        this.hasSAC = Verdict.UNKNOWN
        this.hasBAC = Verdict.UNKNOWN
        this.hasAA = Verdict.UNKNOWN
        this.hasEAC = Verdict.UNKNOWN
        this.hasCA = Verdict.UNKNOWN
    }

    fun setSAC(hasSAC: Verdict) {
        this.hasSAC = hasSAC
    }

    fun hasSAC(): Verdict? {
        return hasSAC
    }


    fun setBAC(hasBAC: Verdict) {
        this.hasBAC = hasBAC
    }

    fun hasBAC(): Verdict? {
        return hasBAC
    }

    fun setAA(hasAA: Verdict) {
        this.hasAA = hasAA
    }

    fun hasAA(): Verdict? {
        return hasAA
    }

    fun setEAC(hasEAC: Verdict) {
        this.hasEAC = hasEAC
    }

    fun hasEAC(): Verdict? {
        return hasEAC
    }

    fun setCA(hasCA: Verdict) {
        this.hasCA = hasCA
    }

    fun hasCA(): Verdict? {
        return hasCA
    }

    constructor(`in`: Parcel) {
        this.hasSAC = Verdict.valueOf(`in`.readString()!!)
        this.hasBAC = Verdict.valueOf(`in`.readString()!!)
        this.hasAA = Verdict.valueOf(`in`.readString()!!)
        this.hasEAC = Verdict.valueOf(`in`.readString()!!)
        this.hasCA = Verdict.valueOf(`in`.readString()!!)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.hasSAC!!.name)
        dest.writeString(this.hasBAC!!.name)
        dest.writeString(this.hasAA!!.name)
        dest.writeString(this.hasEAC!!.name)
        dest.writeString(this.hasCA!!.name)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator<FeatureStatus> {
            override fun createFromParcel(pc: Parcel): FeatureStatus {
                return FeatureStatus(pc)
            }

            override fun newArray(size: Int): Array<FeatureStatus?> {
                return arrayOfNulls(size)
            }
        }
    }
}
