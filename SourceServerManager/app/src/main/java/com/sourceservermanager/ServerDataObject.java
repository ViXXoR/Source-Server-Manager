package com.sourceservermanager;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Matthew on 1/27/2016.
 */
public class ServerDataObject implements Parcelable {

    private long mID;
    private String mNickname;
    private String mHost;
    private String mPort;
    private String mPassword;

    ServerDataObject (long id, String nickname, String host, String port, String password) {
        mID = id;
        mNickname = nickname;
        mHost = host;
        mPort = port;
        mPassword = password;
    }

    ServerDataObject () {
        mID = 0;
        mNickname = "";
        mHost = "";
        mPort = "";
        mPassword = "";
    }

    public long getID() {
        return mID;
    }

    public void setID(long value) {
        this.mID = value;
    }

    public String getNickname() {
        return mNickname;
    }

    public void setNickname(String value) {
        this.mNickname = value;
    }

    public String getHost() {
        return mHost;
    }

    public void setHost(String value) {
        this.mHost = value;
    }

    public String getPort() {
        return mPort;
    }

    public void setPort(String value) {
        this.mPort = value;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String value) {
        this.mPassword = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mID);
        dest.writeString(mNickname);
        dest.writeString(mHost);
        dest.writeString(mPort);
        dest.writeString(mPassword);
    }

    public static final Parcelable.Creator<ServerDataObject> CREATOR = new Creator<ServerDataObject>() {
        public ServerDataObject createFromParcel(Parcel source) {
            ServerDataObject mSDO = new ServerDataObject();

            mSDO.setID(source.readLong());
            mSDO.setNickname(source.readString());
            mSDO.setHost(source.readString());
            mSDO.setPort(source.readString());
            mSDO.setPassword(source.readString());

            return mSDO;
        }
        public ServerDataObject[] newArray(int size) {
            return new ServerDataObject[size];
        }
    };
}
