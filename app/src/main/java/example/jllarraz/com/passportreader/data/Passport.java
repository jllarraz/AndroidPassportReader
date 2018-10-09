package example.jllarraz.com.passportreader.data;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Passport implements Parcelable {

    boolean isChipAuthentication = false;
    boolean isEAC = false;

    Bitmap face;
    Bitmap portrait;
    PersonDetails personDetails;
    AdditionalPersonDetails additionalPersonDetails;

    public Passport(Parcel in) {
        this.face = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.portrait = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.personDetails = in.readInt()== 1 ? in.readParcelable(PersonDetails.class.getClassLoader()) : null;
        this.additionalPersonDetails = in.readInt()== 1 ? in.readParcelable(AdditionalPersonDetails.class.getClassLoader()) : null;
        this.isChipAuthentication = in.readInt()==1;
        this.isEAC = in.readInt()==1;
    }

    public Passport(){
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

    public boolean isChipAuthentication() {
        return isChipAuthentication;
    }

    public void setChipAuthentication(boolean chipAuthentication) {
        isChipAuthentication = chipAuthentication;
    }

    public boolean isEAC() {
        return isEAC;
    }

    public void setEAC(boolean EAC) {
        isEAC = EAC;
    }

    public AdditionalPersonDetails getAdditionalPersonDetails() {
        return additionalPersonDetails;
    }

    public void setAdditionalPersonDetails(AdditionalPersonDetails additionalPersonDetails) {
        this.additionalPersonDetails = additionalPersonDetails;
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

        dest.writeInt(isChipAuthentication?1:0);
        dest.writeInt(isEAC?1:0);
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
