package example.jllarraz.com.passportreader.data;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Passport implements Parcelable {

    boolean isBAC = false;
    boolean isPACE = false;
    boolean isChipAuthentication = false;

    Bitmap face;
    Bitmap portrait;
    Bitmap signature;
    List<Bitmap> fingerprints;
    PersonDetails personDetails;
    AdditionalPersonDetails additionalPersonDetails;
    AdditionalDocumentDetails additionalDocumentDetails;

    public Passport(Parcel in) {
        fingerprints = new ArrayList<>();
        this.face = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.portrait = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.personDetails = in.readInt()== 1 ? in.readParcelable(PersonDetails.class.getClassLoader()) : null;
        this.additionalPersonDetails = in.readInt()== 1 ? in.readParcelable(AdditionalPersonDetails.class.getClassLoader()) : null;
        this.isBAC = in.readInt()==1;
        this.isPACE = in.readInt()==1;
        if(in.readInt()==1){
            in.readList(fingerprints, Bitmap.class.getClassLoader());
        }

        this.signature = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.additionalDocumentDetails = in.readInt()== 1 ? in.readParcelable(AdditionalDocumentDetails.class.getClassLoader()) : null;
        this.isChipAuthentication = in.readInt()==1;
    }

    public Passport(){
        fingerprints = new ArrayList<>();
    }

    public Bitmap getFace() {
        return face;
    }

    public void setFace(Bitmap face) {
        this.face = face;
    }

    public Bitmap getPortrait() {
        return portrait;
    }

    public void setPortrait(Bitmap portrait) {
        this.portrait = portrait;
    }

    public PersonDetails getPersonDetails() {
        return personDetails;
    }

    public void setPersonDetails(PersonDetails personDetails) {
        this.personDetails = personDetails;
    }

    public boolean isBAC() {
        return isBAC;
    }

    public void setBAC(boolean BAC) {
        isBAC = BAC;
    }

    public boolean isPACE() {
        return isPACE;
    }

    public void setPACE(boolean PACE) {
        isPACE = PACE;
    }

    public boolean isChipAuthentication() {
        return isChipAuthentication;
    }

    public void setChipAuthentication(boolean chipAuthentication) {
        isChipAuthentication = chipAuthentication;
    }

    public List<Bitmap> getFingerprints() {
        return fingerprints;
    }

    public void setFingerprints(List<Bitmap> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public Bitmap getSignature() {
        return signature;
    }

    public void setSignature(Bitmap signature) {
        this.signature = signature;
    }

    public AdditionalPersonDetails getAdditionalPersonDetails() {
        return additionalPersonDetails;
    }

    public void setAdditionalPersonDetails(AdditionalPersonDetails additionalPersonDetails) {
        this.additionalPersonDetails = additionalPersonDetails;
    }

    public AdditionalDocumentDetails getAdditionalDocumentDetails() {
        return additionalDocumentDetails;
    }

    public void setAdditionalDocumentDetails(AdditionalDocumentDetails additionalDocumentDetails) {
        this.additionalDocumentDetails = additionalDocumentDetails;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(face!=null ? 1 : 0);
        if(face!=null) {
            dest.writeParcelable(face, flags);
        }

        dest.writeInt(portrait!=null ? 1 : 0);
        if(portrait!=null) {
            dest.writeParcelable(portrait, flags);
        }

        dest.writeInt(personDetails!=null ? 1 : 0);
        if(personDetails!=null) {
            dest.writeParcelable(personDetails, flags);
        }

        dest.writeInt(additionalPersonDetails!=null ? 1 : 0);
        if(additionalPersonDetails!=null) {
            dest.writeParcelable(additionalPersonDetails, flags);
        }

        dest.writeInt(isBAC ?1:0);
        dest.writeInt(isPACE ?1:0);

        dest.writeInt(fingerprints!=null ? 1 : 0);
        if(fingerprints!=null) {
            dest.writeList(fingerprints);
        }

        dest.writeInt(signature!=null ? 1 : 0);
        if(signature!=null) {
            dest.writeParcelable(signature, flags);
        }

        dest.writeInt(additionalDocumentDetails!=null ? 1 : 0);
        if(additionalDocumentDetails!=null) {
            dest.writeParcelable(additionalDocumentDetails, flags);
        }

        dest.writeInt(isChipAuthentication ?1:0);
    }


    public static final Creator CREATOR = new Creator<Passport>() {
        public Passport createFromParcel(Parcel pc) {
            return new Passport(pc);
        }

        public Passport[] newArray(int size) {
            return new Passport[size];
        }
    };
}
