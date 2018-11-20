package org.jmrtd;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Security features of this identity document.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
public class FeatureStatus implements Parcelable {

    /**
     * Outcome of a feature presence check.
     *
     * @author The JMRTD team (info@jmrtd.org)
     *
     * @version $Revision: 1559 $
     */
    public enum Verdict {
        UNKNOWN,		/* Presence unknown */
        PRESENT,		/* Present */
        NOT_PRESENT;	/* Not present */
    };

    private Verdict hasSAC, hasBAC, hasAA, hasEAC, hasCA;

    public FeatureStatus() {
        this.hasSAC = Verdict.UNKNOWN;
        this.hasBAC = Verdict.UNKNOWN;
        this.hasAA = Verdict.UNKNOWN;
        this.hasEAC = Verdict.UNKNOWN;
        this.hasCA = Verdict.UNKNOWN;
    }

    public void setSAC(Verdict hasSAC) {
        this.hasSAC = hasSAC;
    }

    public Verdict hasSAC() {
        return hasSAC;
    }


    public void setBAC(Verdict hasBAC) {
        this.hasBAC = hasBAC;
    }

    public Verdict hasBAC() {
        return hasBAC;
    }

    public void setAA(Verdict hasAA) {
        this.hasAA = hasAA;
    }

    public Verdict hasAA() {
        return hasAA;
    }

    public void setEAC(Verdict hasEAC) {
        this.hasEAC = hasEAC;
    }

    public Verdict hasEAC() {
        return hasEAC;
    }

    public void setCA(Verdict hasCA) {
        this.hasCA = hasCA;
    }

    public Verdict hasCA() {
        return hasCA;
    }

    public FeatureStatus(Parcel in) {
        this.hasSAC=Verdict.valueOf(in.readString());
        this.hasBAC=Verdict.valueOf(in.readString());
        this.hasAA=Verdict.valueOf(in.readString());
        this.hasEAC=Verdict.valueOf(in.readString());
        this.hasCA=Verdict.valueOf(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.hasSAC.name());
        dest.writeString(this.hasBAC.name());
        dest.writeString(this.hasAA.name());
        dest.writeString(this.hasEAC.name());
        dest.writeString(this.hasCA.name());
    }

    public static final Creator CREATOR = new Creator<FeatureStatus>() {
        public FeatureStatus createFromParcel(Parcel pc) {
            return new FeatureStatus(pc);
        }

        public FeatureStatus[] newArray(int size) {
            return new FeatureStatus[size];
        }
    };
}
