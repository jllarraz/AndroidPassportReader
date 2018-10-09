package example.jllarraz.com.passportreader.data;

import android.os.Parcel;
import android.os.Parcelable;

import net.sf.scuba.data.Gender;

import org.jmrtd.lds.icao.MRZInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PersonDetails implements Parcelable {


    private String documentCode;
    private String issuingState;
    private String primaryIdentifier;
    private String secondaryIdentifier;
    private String nationality;
    private String documentNumber;
    private String dateOfBirth;
    private String dateOfExpiry;
    private String optionalData1; /* NOTE: holds personal number for some issuing states (e.g. NL), but is used to hold (part of) document number for others. */
    private String optionalData2;
    private Gender gender=Gender.UNKNOWN;

    public PersonDetails(){
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public void setDocumentCode(String documentCode) {
        this.documentCode = documentCode;
    }

    public String getIssuingState() {
        return issuingState;
    }

    public void setIssuingState(String issuingState) {
        this.issuingState = issuingState;
    }

    public String getPrimaryIdentifier() {
        return primaryIdentifier;
    }

    public void setPrimaryIdentifier(String primaryIdentifier) {
        this.primaryIdentifier = primaryIdentifier;
    }

    public String getSecondaryIdentifier() {
        return secondaryIdentifier;
    }

    public void setSecondaryIdentifier(String secondaryIdentifier) {
        this.secondaryIdentifier = secondaryIdentifier;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getDateOfExpiry() {
        return dateOfExpiry;
    }

    public void setDateOfExpiry(String dateOfExpiry) {
        this.dateOfExpiry = dateOfExpiry;
    }

    public String getOptionalData1() {
        return optionalData1;
    }

    public void setOptionalData1(String optionalData1) {
        this.optionalData1 = optionalData1;
    }

    public String getOptionalData2() {
        return optionalData2;
    }

    public void setOptionalData2(String optionalData2) {
        this.optionalData2 = optionalData2;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public PersonDetails(Parcel in) {


        this.documentCode = in.readInt()== 1 ? in.readString() : null;
        this.issuingState = in.readInt()== 1 ? in.readString() : null;
        this.primaryIdentifier = in.readInt()== 1 ? in.readString() : null;
        this.secondaryIdentifier = in.readInt()== 1 ? in.readString() : null;
        this.nationality = in.readInt()== 1 ? in.readString() : null;
        this.documentNumber = in.readInt()== 1 ? in.readString() : null;
        this.dateOfBirth = in.readInt()== 1 ? in.readString() : null;
        this.dateOfExpiry = in.readInt()== 1 ? in.readString() : null;
        this.optionalData1 = in.readInt()== 1 ? in.readString() : null;
        this.optionalData2 = in.readInt()== 1 ? in.readString() : null;
        this.gender = in.readInt()== 1 ? Gender.valueOf(in.readString()) : Gender.UNKNOWN;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(documentCode!=null ? 1 : 0);
        if(documentCode!=null) {
            dest.writeString(documentCode);
        }

        dest.writeInt(issuingState!=null ? 1 : 0);
        if(issuingState!=null) {
            dest.writeString(issuingState);
        }

        dest.writeInt(primaryIdentifier!=null ? 1 : 0);
        if(primaryIdentifier!=null) {
            dest.writeString(primaryIdentifier);
        }

        dest.writeInt(secondaryIdentifier!=null ? 1 : 0);
        if(secondaryIdentifier!=null) {
            dest.writeString(secondaryIdentifier);
        }

        dest.writeInt(nationality!=null ? 1 : 0);
        if(nationality!=null) {
            dest.writeString(nationality);
        }

        dest.writeInt(documentNumber!=null ? 1 : 0);
        if(documentNumber!=null) {
            dest.writeString(documentNumber);
        }

        dest.writeInt(dateOfBirth!=null ? 1 : 0);
        if(dateOfBirth!=null) {
            dest.writeString(dateOfBirth);
        }

        dest.writeInt(dateOfExpiry!=null ? 1 : 0);
        if(dateOfExpiry!=null) {
            dest.writeString(dateOfExpiry);
        }

        dest.writeInt(optionalData1!=null ? 1 : 0);
        if(optionalData1!=null) {
            dest.writeString(optionalData1);
        }

        dest.writeInt(optionalData2!=null ? 1 : 0);
        if(optionalData2!=null) {
            dest.writeString(optionalData2);
        }

        dest.writeInt(gender!=null ? 1 : 0);
        if(optionalData2!=null) {
            dest.writeString(String.valueOf(gender.name()));
        }


    }

    public static final Creator CREATOR = new Creator<PersonDetails>() {
        public PersonDetails createFromParcel(Parcel pc) {
            return new PersonDetails(pc);
        }

        public PersonDetails[] newArray(int size) {
            return new PersonDetails[size];
        }
    };
}
