package android.garda.ie.mylibrary.test;

/**
 * Security features of this identity document.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
public class FeatureStatus {

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

    private Verdict hasSAC, hasBAC, hasAA, hasEAC;

    public FeatureStatus() {
        this.hasSAC = Verdict.UNKNOWN;
        this.hasBAC = Verdict.UNKNOWN;
        this.hasAA = Verdict.UNKNOWN;
        this.hasEAC = Verdict.UNKNOWN;
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
}
