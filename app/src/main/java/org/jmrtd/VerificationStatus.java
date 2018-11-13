package org.jmrtd;

import net.sf.scuba.util.Hex;

import org.jmrtd.protocol.EACCAResult;
import org.jmrtd.protocol.EACTAResult;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A data type for communicating document verification check information.
 *
 * @author The JMRTD team (info@jmrtd.org)
 *
 * @version $Revision: 1559 $
 */
public class VerificationStatus {

    /**
     * Outcome of a verification process.
     *
     * @author The JMRTD team (info@jmrtd.org)
     *
     * @version $Revision: 1559 $
     */
    public enum Verdict {
        UNKNOWN,		/* Unknown */
        NOT_PRESENT,	/* Not present */
        NOT_CHECKED,	/* Present, not checked */
        FAILED,			/* Present, checked, and not ok */
        SUCCEEDED;		/* Present, checked, and ok */
    };

    /* Verdict for this verification feature. */
    private Verdict aa, bac, sac, cs, ht, ds, eac;

    /* Textual reason for the verdict. */
    private String aaReason, bacReason, sacReason, csReason, htReason, dsReason, eacReason;

    /* By products of the verification process that may be useful for relying parties to display. */
    private List triedBACEntries; /* As a result of BAC testing, this contains all tried BAC entries. */
    private Map hashResults; /* As a result of HT testing, this contains stored and computed hashes. */
    private List certificateChain; /* As a result of CS testing, this contains certificate chain from DSC to CSCA. */
    private EACTAResult eacResult;

    /**
     * Constructs a new status with all verdicts
     * set to UNKNOWN.
     */
    public VerificationStatus() {
        setAll(Verdict.UNKNOWN, null);
    }

    /**
     * Gets the AA verdict.
     *
     * @return the AA status
     */
    public Verdict getAA() {
        return aa;
    }

    /**
     * Gets the AA reason string.
     *
     * @return a reason string
     */
    public String getAAReason() {
        return aaReason;
    }

    /**
     * Sets the AA verdict.
     *
     * @param v the status to set
     * @param reason a reason string
     */
    public void setAA(Verdict v, String reason) {
        this.aa = v;
        this.aaReason = reason;
    }

    /**
     * Gets the BAC verdict.
     *
     * @return the BAC status
     */
    public Verdict getBAC() {
        return bac;
    }

    /**
     * Gets the BAC verdict string.
     *
     * @return a verdict string
     */
    public String getBACReason() {
        return bacReason;
    }

    /**
     * Gets the tried BAC entries.
     *
     * @return a list of BAC keys
     */
    public List getTriedBACEntries() {
        return triedBACEntries;
    }

    /**
     * Sets the BAC verdict.
     *
     * @param v the status to set
     * @param reason a reason string
     * @param triedBACEntries the list of BAC entries that were tried
     */
    public void setBAC(Verdict v, String reason, List triedBACEntries) {
        this.bac = v;
        this.bacReason = reason;
        this.triedBACEntries = triedBACEntries;
    }

    /**
     * Gets the SAC verdict.
     *
     * @return the SAC verdict
     */
    public Verdict getSAC() {
        return sac;
    }

    /**
     * Gets the SAC reason.
     *
     * @return a reason string
     */
    public String getSACReason() {
        return sacReason;
    }

    /**
     * Sets the SAC verdict and reason string.
     *
     * @param v a verdict
     * @param reason a reason string
     */
    public void setSAC(Verdict v, String reason) {
        this.sac = v;
        this.sacReason = reason;
    }

    /**
     * Gets the CS verdict.
     *
     * @return the CS status
     */
    public Verdict getCS() {
        return cs;
    }

    /**
     * Gets the country signature reason string.
     *
     * @return a reason string
     */
    public String getCSReason() {
        return csReason;
    }

    /**
     * Gets the certificate chain between DS and CSCA.
     *
     * @return a certificate chain
     */
    public List getCertificateChain() {
        return certificateChain;
    }

    /**
     * Gets the CS verdict.
     *
     * @param v the status to set
     * @param reason the reason string
     * @param certificateChain the certificate chain between DS and CSCA
     */
    public void setCS(Verdict v, String reason, List certificateChain) {
        this.cs = v;
        this.csReason = reason;
        this.certificateChain = certificateChain;
    }

    /**
     * Gets the DS verdict.
     *
     * @return the DS status
     */
    public Verdict getDS() {
        return ds;
    }

    /**
     * Gets the document signature verdict reason string.
     *
     * @return a reason string
     */
    public String getDSReason() {
        return dsReason;
    }


    /**
     * Sets the DS verdict.
     *
     * @param v the status to set
     * @param reason reason string
     */
    public void setDS(Verdict v, String reason) {
        this.ds = v;
        this.dsReason = reason;
    }

    /**
     * Gets the hash table verdict.
     *
     * @return a verdict
     */
    public Verdict getHT() {
        return ht;
    }

    /**
     * Gets the hash table reason string.
     *
     * @return a reason string
     */
    public String getHTReason() {
        return htReason;
    }

    /**
     * Gets the hash match results.
     *
     * @return a list of hash match results
     */
    public Map getHashResults() {
        return hashResults;
    }

    /**
     * Sets the hash table status.
     *
     * @param v a verdict
     * @param reason the reason string
     * @param hashResults the hash match results
     */
    public void setHT(Verdict v, String reason, Map hashResults) {
        this.ht = v;
        this.htReason = reason;
        this.hashResults = hashResults;
    }

    /**
     * Gets the EAC verdict.
     *
     * @return the EAC status
     */
    public Verdict getEAC() {
        return eac;
    }

    /**
     * Gets the EAC reason string.
     *
     * @return a reasons string
     */
    public String getEACReason() {
        return eacReason;
    }

    /**
     * Gets the EAC result.
     *
     * @return the EAC result
     */
    public EACTAResult getEACResult() {
        return eacResult;
    }

    /**
     * Sets the EAC verdict.
     *
     * @param v the status to set
     * @param eacResult the EAC result
     * @param reason reason string
     */
    public void setEAC(Verdict v, String reason, EACTAResult eacResult) {
        this.eac = v;
        this.eacReason = reason;
        this.eacResult = eacResult;
    }

    /**
     * Sets all vedicts to v.
     *
     * @param verdict the status to set
     * @param reason reason string
     */
    public void setAll(Verdict verdict, String reason) {
        setAA(verdict, reason);
        setBAC(verdict, reason, null);
        setCS(verdict, reason, null);
        setDS(verdict, reason);
        setHT(verdict, reason, null);
        setEAC(verdict, reason, null);
    }

    /**
     * The result of matching the stored and computed hashes of a single datagroup.
     *
     * FIXME: perhaps that boolean should be more like verdict, including a reason for mismatch if known (e.g. access denied for EAC datagroup) -- MO
     */
    public static class HashMatchResult implements Serializable {

        private static final long serialVersionUID = 263961258911936111L;

        private byte[] storedHash, computedHash;

        /**
         * Use null for computed hash if access was denied.
         *
         * @param storedHash the hash stored in SOd
         * @param computedHash the computed hash
         */
        public HashMatchResult(byte[] storedHash, byte[] computedHash) {
            this.storedHash = storedHash;
            this.computedHash = computedHash;
        }

        /**
         * Gets the stored hash.
         *
         * @return a hash
         */
        public byte[] getStoredHash() {
            return storedHash;
        }

        /**
         * Gets the computed hash.
         *
         * @return a hash
         */
        public byte[] getComputedHash() {
            return computedHash;
        }

        /**
         * Whether the hashes match.
         *
         * @return a boolean
         */
        public boolean isMatch() {
            return Arrays.equals(storedHash, computedHash);
        }

        public String toString() {
            return "HashResult [" + isMatch() + ", stored: " + Hex.bytesToHexString(storedHash) + ", computed: " + Hex.bytesToHexString(computedHash);
        }

        public int hashCode() {
            return 11 + 3 * Arrays.hashCode(storedHash) + 5 * Arrays.hashCode(computedHash);
        }

        public boolean equals(Object other) {
            if (other == null) { return false; }
            if (other == this) { return true; }
            if (!other.getClass().equals(this.getClass())) { return false; }
            HashMatchResult otherHashResult = (HashMatchResult)other;
            return Arrays.equals(otherHashResult.computedHash, computedHash)
                    && Arrays.equals(otherHashResult.storedHash, storedHash);
        }

        /* NOTE: Part of our serializable implementation. */
        private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
//			inputStream.defaultReadObject();
            storedHash = readBytes(inputStream);
            computedHash = readBytes(inputStream);
        }

        /* NOTE: Part of our serializable implementation. */
        private void writeObject(ObjectOutputStream outputStream) throws IOException {
//			outputStream.defaultWriteObject();
            writeByteArray(storedHash, outputStream);
            writeByteArray(computedHash, outputStream);
        }

        private byte[] readBytes(ObjectInputStream inputStream) throws IOException {
            int length = inputStream.readInt();
            if (length < 0) {
                return null;
            }
            byte[] bytes = new byte[length];
            for (int i = 0; i < length; i++) {
                int b = inputStream.readInt();
                bytes[i] = (byte)b;
            }
            return bytes;
        }

        private void writeByteArray(byte[] bytes, ObjectOutputStream outputStream) throws IOException {
            if (bytes == null) {
                outputStream.writeInt(-1);
            } else {
                outputStream.writeInt(bytes.length);
                for (byte b: bytes) {
                    outputStream.writeInt(b);
                }
            }
        }
    }
}
