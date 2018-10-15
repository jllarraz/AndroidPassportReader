package example.jllarraz.com.passportreader.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdditionalPersonDetails implements Parcelable {

    String custodyInformation;
    String fullDateOfBirth;
    String nameOfHolder;
    List<String> otherNames;
    List<String> otherValidTDNumbers;
    List<String> permanentAddress;
    String personalNumber;
    String personalSummary;
    List<String> placeOfBirth;
    String profession;
    byte[] proofOfCitizenship;
    int tag;
    List<Integer> tagPresenceList;
    String telephone;
    String title;

    public AdditionalPersonDetails(){
        otherNames = new ArrayList<>();
        otherValidTDNumbers = new ArrayList<>();
        permanentAddress = new ArrayList<>();
        placeOfBirth = new ArrayList<>();
        tagPresenceList = new ArrayList<>();
    }

    public String getCustodyInformation() {
        return custodyInformation;
    }

    public void setCustodyInformation(String custodyInformation) {
        this.custodyInformation = custodyInformation;
    }

    public String getFullDateOfBirth() {
        return fullDateOfBirth;
    }

    public void setFullDateOfBirth(String fullDateOfBirth) {
        this.fullDateOfBirth = fullDateOfBirth;
    }

    public String getNameOfHolder() {
        return nameOfHolder;
    }

    public void setNameOfHolder(String nameOfHolder) {
        this.nameOfHolder = nameOfHolder;
    }

    public List<String> getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(List<String> otherNames) {
        this.otherNames = otherNames;
    }

    public List<String> getOtherValidTDNumbers() {
        return otherValidTDNumbers;
    }

    public void setOtherValidTDNumbers(List<String> otherValidTDNumbers) {
        this.otherValidTDNumbers = otherValidTDNumbers;
    }

    public List<String> getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(List<String> permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }

    public String getPersonalSummary() {
        return personalSummary;
    }

    public void setPersonalSummary(String personalSummary) {
        this.personalSummary = personalSummary;
    }

    public List<String> getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(List<String> placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public byte[] getProofOfCitizenship() {
        return proofOfCitizenship;
    }

    public void setProofOfCitizenship(byte[] proofOfCitizenship) {
        this.proofOfCitizenship = proofOfCitizenship;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AdditionalPersonDetails(Parcel in) {

        otherNames = new ArrayList<>();
        otherValidTDNumbers = new ArrayList<>();
        permanentAddress = new ArrayList<>();
        placeOfBirth = new ArrayList<>();
        tagPresenceList = new ArrayList<>();

        this.custodyInformation = in.readInt()== 1 ? in.readString() : null;
        this.fullDateOfBirth = in.readInt()== 1 ? in.readString() : null;
        this.nameOfHolder = in.readInt()== 1 ? in.readString() : null;
        if(in.readInt() == 1){
            in.readList(otherNames, String.class.getClassLoader());
        }
        if(in.readInt() == 1){
            in.readList(otherValidTDNumbers, String.class.getClassLoader());
        }
        if(in.readInt() == 1){
            in.readList(permanentAddress, String.class.getClassLoader());
        }
        this.personalNumber = in.readInt()== 1 ? in.readString() : null;
        this.personalSummary = in.readInt()== 1 ? in.readString() : null;
        if(in.readInt() == 1){
            in.readList(placeOfBirth, String.class.getClassLoader());
        }
        this.profession = in.readInt()== 1 ? in.readString() : null;
        if( in.readInt()== 1){
            this.proofOfCitizenship = new byte[in.readInt()];
            in.readByteArray(this.proofOfCitizenship);
        }
        tag = in.readInt();
        if(in.readInt() == 1){
            in.readList(tagPresenceList, Integer.class.getClassLoader());
        }

        this.telephone = in.readInt()== 1 ? in.readString() : null;
        this.title = in.readInt()== 1 ? in.readString() : null;


    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(custodyInformation!=null ? 1 : 0);
        if(custodyInformation!=null) {
            dest.writeString(custodyInformation);
        }

        dest.writeInt(fullDateOfBirth!=null ? 1 : 0);
        if(fullDateOfBirth!=null) {
            dest.writeString(fullDateOfBirth);
        }


        dest.writeInt(nameOfHolder!=null ? 1 : 0);
        if(nameOfHolder!=null) {
            dest.writeString(nameOfHolder);
        }
        dest.writeInt(otherNames!=null ? 1 : 0);
        if(otherNames!=null) {
            dest.writeList(otherNames);
        }

        dest.writeInt(otherValidTDNumbers!=null ? 1 : 0);
        if(otherValidTDNumbers!=null) {
            dest.writeList(otherValidTDNumbers);
        }

        dest.writeInt(permanentAddress!=null ? 1 : 0);
        if(permanentAddress!=null) {
            dest.writeList(permanentAddress);
        }

        dest.writeInt(personalNumber!=null ? 1 : 0);
        if(personalNumber!=null) {
            dest.writeString(personalNumber);
        }

        dest.writeInt(personalSummary!=null ? 1 : 0);
        if(personalSummary!=null) {
            dest.writeString(personalSummary);
        }

        dest.writeInt(placeOfBirth!=null ? 1 : 0);
        if(placeOfBirth!=null) {
            dest.writeList(placeOfBirth);
        }

        dest.writeInt(profession!=null ? 1 : 0);
        if(profession!=null) {
            dest.writeString(profession);
        }

        dest.writeInt(proofOfCitizenship!=null ? 1 : 0);
        if(proofOfCitizenship!=null) {
            dest.writeInt(proofOfCitizenship.length);
            dest.writeByteArray(proofOfCitizenship);
        }

        dest.writeInt(tag);
        dest.writeInt(tagPresenceList!=null ? 1 : 0);
        if(tagPresenceList!=null) {
            dest.writeList(tagPresenceList);
        }

        dest.writeInt(telephone!=null ? 1 : 0);
        if(telephone!=null) {
            dest.writeString(telephone);
        }

        dest.writeInt(title!=null ? 1 : 0);
        if(title!=null) {
            dest.writeString(title);
        }

    }

    public static final Creator CREATOR = new Creator<AdditionalPersonDetails>() {
        public AdditionalPersonDetails createFromParcel(Parcel pc) {
            return new AdditionalPersonDetails(pc);
        }

        public AdditionalPersonDetails[] newArray(int size) {
            return new AdditionalPersonDetails[size];
        }
    };
}
