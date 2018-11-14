package example.jllarraz.com.passportreader.data;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import org.jmrtd.FeatureStatus;
import org.jmrtd.VerificationStatus;
import org.jmrtd.lds.SODFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Passport implements Parcelable {

    private SODFile sodFile;
    private Bitmap face;
    private Bitmap portrait;
    private Bitmap signature;
    private List<Bitmap> fingerprints;
    private PersonDetails personDetails;
    private AdditionalPersonDetails additionalPersonDetails;
    private AdditionalDocumentDetails additionalDocumentDetails;
    private FeatureStatus featureStatus;
    private VerificationStatus verificationStatus;

    public Passport(Parcel in) {


        fingerprints = new ArrayList<>();
        this.face = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.portrait = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.personDetails = in.readInt()== 1 ? in.readParcelable(PersonDetails.class.getClassLoader()) : null;
        this.additionalPersonDetails = in.readInt()== 1 ? in.readParcelable(AdditionalPersonDetails.class.getClassLoader()) : null;

        if(in.readInt()==1){
            in.readList(fingerprints, Bitmap.class.getClassLoader());
        }

        this.signature = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.additionalDocumentDetails = in.readInt()== 1 ? in.readParcelable(AdditionalDocumentDetails.class.getClassLoader()) : null;
        if(in.readInt()==1){
            sodFile = (SODFile) in.readSerializable();
        }

        if(in.readInt()==1){
            featureStatus = in.readParcelable(FeatureStatus.class.getClassLoader());
        }

        if(in.readInt()==1){
            featureStatus = in.readParcelable(FeatureStatus.class.getClassLoader());
        }

        if(in.readInt()==1){
            verificationStatus = in.readParcelable(VerificationStatus.class.getClassLoader());
        }

    }

    public Passport(){
        fingerprints = new ArrayList<>();
        featureStatus=new FeatureStatus();
        verificationStatus = new VerificationStatus();
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

    public SODFile getSodFile() {
        return sodFile;
    }

    public void setSodFile(SODFile sodFile) {
        this.sodFile = sodFile;
    }

    public FeatureStatus getFeatureStatus() {
        return featureStatus;
    }

    public void setFeatureStatus(FeatureStatus featureStatus) {
        this.featureStatus = featureStatus;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
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

        dest.writeInt(sodFile!=null ? 1 : 0);
        if(sodFile!=null) {
            dest.writeSerializable(sodFile);
        }

        dest.writeInt(featureStatus!=null ? 1 : 0);
        if(featureStatus!=null) {
            dest.writeParcelable(featureStatus, flags);
        }

        dest.writeInt(verificationStatus!=null ? 1 : 0);
        if(verificationStatus!=null) {
            dest.writeParcelable(verificationStatus, flags);
        }

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
