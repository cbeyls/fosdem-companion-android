package org.fossasia.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.fossasia.db.DatabaseHelper;


/**
 * Created by Abhishek on 17/02/15.
 */
public class Speaker implements Parcelable {

    public static final Parcelable.Creator<Speaker> CREATOR = new Parcelable.Creator<Speaker>() {
        public Speaker createFromParcel(Parcel in) {
            return new Speaker(in);
        }

        public Speaker[] newArray(int size) {
            return new Speaker[size];
        }
    };
    private int id;
    private String name;
    private String information;
    private String linkedInUrl;
    private String twitterHandle;
    private String designation;
    private String profilePicUrl;
    private boolean isKeySpeaker;

    public Speaker(int id, String name, String information, String linkedInUrl, String twitterHandle, String designation, String profilePicUrl, int isKeySpeaker) {
        this.id = id;
        this.name = name;
        this.information = information;
        this.linkedInUrl = linkedInUrl;
        this.twitterHandle = twitterHandle;
        this.designation = designation;
        this.profilePicUrl = profilePicUrl;
        if (isKeySpeaker == 0) {
            this.isKeySpeaker = false;
        } else {
            this.isKeySpeaker = true;
        }
    }

    public Speaker(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.information = in.readString();
        this.linkedInUrl = in.readString();
        this.twitterHandle = in.readString();
        this.designation = in.readString();
        this.profilePicUrl = in.readString();
        this.isKeySpeaker = in.readByte() != 0;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    public String getTwitterHandle() {
        return twitterHandle;
    }

    public void setTwitterHandle(String twitterHandle) {
        this.twitterHandle = twitterHandle;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public boolean isKeySpeaker() {
        return isKeySpeaker;
    }

    public String generateSqlQuery() {
        String query = String.format("INSERT OR IGNORE INTO %s VALUES (%d, '%s', '%s', '%s', '%s', '%s', '%s', %d);", DatabaseHelper.TABLE_NAME_KEY_SPEAKERS, id, name, designation, information, twitterHandle, linkedInUrl, profilePicUrl, (isKeySpeaker ? 1 : 0));
        return query;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeString(name);
        out.writeString(information);
        out.writeString(linkedInUrl);
        out.writeString(twitterHandle);
        out.writeString(designation);
        out.writeString(profilePicUrl);
        out.writeByte((byte) (isKeySpeaker ? 1 : 0));

    }
}
