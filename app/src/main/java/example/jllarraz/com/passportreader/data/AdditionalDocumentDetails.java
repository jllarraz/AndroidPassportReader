package example.jllarraz.com.passportreader.data;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdditionalDocumentDetails implements Parcelable {

    String endorsementsAndObservations;
    String dateAndTimeOfPersonalization;
    String dateOfIssue;
    Bitmap imageOfFront;
    Bitmap imageOfRear;
    String issuingAuthority;
    List<String> namesOfOtherPersons;
    String personalizationSystemSerialNumber;
    String taxOrExitRequirements;
    int tag;
    List<Integer> tagPresenceList;


    public AdditionalDocumentDetails(){
        namesOfOtherPersons = new ArrayList<>();
        tagPresenceList = new ArrayList<>();
    }

    public String getEndorsementsAndObservations() {
        return endorsementsAndObservations;
    }

    public void setEndorsementsAndObservations(String endorsementsAndObservations) {
        this.endorsementsAndObservations = endorsementsAndObservations;
    }

    public String getDateAndTimeOfPersonalization() {
        return dateAndTimeOfPersonalization;
    }

    public void setDateAndTimeOfPersonalization(String dateAndTimeOfPersonalization) {
        this.dateAndTimeOfPersonalization = dateAndTimeOfPersonalization;
    }

    public String getDateOfIssue() {
        return dateOfIssue;
    }

    public void setDateOfIssue(String dateOfIssue) {
        this.dateOfIssue = dateOfIssue;
    }

    public Bitmap getImageOfFront() {
        return imageOfFront;
    }

    public void setImageOfFront(Bitmap imageOfFront) {
        this.imageOfFront = imageOfFront;
    }

    public Bitmap getImageOfRear() {
        return imageOfRear;
    }

    public void setImageOfRear(Bitmap imageOfRear) {
        this.imageOfRear = imageOfRear;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public List<String> getNamesOfOtherPersons() {
        return namesOfOtherPersons;
    }

    public void setNamesOfOtherPersons(List<String> namesOfOtherPersons) {
        this.namesOfOtherPersons = namesOfOtherPersons;
    }

    public String getPersonalizationSystemSerialNumber() {
        return personalizationSystemSerialNumber;
    }

    public void setPersonalizationSystemSerialNumber(String personalizationSystemSerialNumber) {
        this.personalizationSystemSerialNumber = personalizationSystemSerialNumber;
    }

    public String getTaxOrExitRequirements() {
        return taxOrExitRequirements;
    }

    public void setTaxOrExitRequirements(String taxOrExitRequirements) {
        this.taxOrExitRequirements = taxOrExitRequirements;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public List<Integer> getTagPresenceList() {
        return tagPresenceList;
    }

    public void setTagPresenceList(List<Integer> tagPresenceList) {
        this.tagPresenceList = tagPresenceList;
    }

    public AdditionalDocumentDetails(Parcel in) {

        namesOfOtherPersons = new ArrayList<>();
        tagPresenceList = new ArrayList<>();

        this.endorsementsAndObservations = in.readInt()== 1 ? in.readString() : null;
        this.dateAndTimeOfPersonalization = in.readInt()== 1 ? in.readString() : null;
        this.dateOfIssue = in.readInt()== 1 ? in.readString() : null;

        this.imageOfFront = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.imageOfRear = in.readInt()== 1 ? in.readParcelable(Bitmap.class.getClassLoader()) : null;
        this.issuingAuthority = in.readInt()== 1 ? in.readString() : null;

        if(in.readInt() == 1){
            in.readList(namesOfOtherPersons, String.class.getClassLoader());
        }

        this.personalizationSystemSerialNumber = in.readInt()== 1 ? in.readString() : null;
        this.taxOrExitRequirements = in.readInt()== 1 ? in.readString() : null;

        tag = in.readInt();
        if(in.readInt() == 1){
            in.readList(tagPresenceList, Integer.class.getClassLoader());
        }



    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(endorsementsAndObservations !=null ? 1 : 0);
        if(endorsementsAndObservations !=null) {
            dest.writeString(endorsementsAndObservations);
        }

        dest.writeInt(dateAndTimeOfPersonalization !=null ? 1 : 0);
        if(dateAndTimeOfPersonalization !=null) {
            dest.writeString(dateAndTimeOfPersonalization);
        }

        dest.writeInt(dateOfIssue !=null ? 1 : 0);
        if(dateOfIssue !=null) {
            dest.writeString(dateOfIssue);
        }

        dest.writeInt(imageOfFront!=null ? 1 : 0);
        if(imageOfFront!=null) {
            dest.writeParcelable(imageOfFront, flags);
        }

        dest.writeInt(imageOfRear!=null ? 1 : 0);
        if(imageOfRear!=null) {
            dest.writeParcelable(imageOfRear, flags);
        }

        dest.writeInt(issuingAuthority !=null ? 1 : 0);
        if(issuingAuthority !=null) {
            dest.writeString(issuingAuthority);
        }

        dest.writeInt(namesOfOtherPersons !=null ? 1 : 0);
        if(namesOfOtherPersons !=null) {
            dest.writeList(namesOfOtherPersons);
        }

        dest.writeInt(personalizationSystemSerialNumber !=null ? 1 : 0);
        if(personalizationSystemSerialNumber !=null) {
            dest.writeString(personalizationSystemSerialNumber);
        }

        dest.writeInt(taxOrExitRequirements !=null ? 1 : 0);
        if(taxOrExitRequirements !=null) {
            dest.writeString(taxOrExitRequirements);
        }

        dest.writeInt(tag);
        dest.writeInt(tagPresenceList!=null ? 1 : 0);
        if(tagPresenceList!=null) {
            dest.writeList(tagPresenceList);
        }


    }

    public static final Creator CREATOR = new Creator<AdditionalDocumentDetails>() {
        public AdditionalDocumentDetails createFromParcel(Parcel pc) {
            return new AdditionalDocumentDetails(pc);
        }

        public AdditionalDocumentDetails[] newArray(int size) {
            return new AdditionalDocumentDetails[size];
        }
    };
}
