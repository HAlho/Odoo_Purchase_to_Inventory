package com.example.projectodoo;

import android.os.Parcel;
import android.os.Parcelable;

public class Credentials implements Parcelable {

    private String database;
    private String username;
    private String password;
    private int id;

    public Credentials(String database, String username, String password, int id) {
        this.database = database;
        this.username = username;
        this.password = password;
        this.id = id;
    }
    public Credentials(String database, String password, int id) {
        this.database = database;
        this.username = username;
        this.password = password;
        this.id = id;
    }
    //parelable implementation
    protected Credentials(Parcel in) {
        database = in.readString();
        username = in.readString();
        password = in.readString();
        id = in.readInt();
    }

    public static final Creator<Credentials> CREATOR = new Creator<Credentials>() {
        @Override
        public Credentials createFromParcel(Parcel in) {
            return new Credentials(in);
        }

        @Override
        public Credentials[] newArray(int size) {
            return new Credentials[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(database);
        parcel.writeString(username);
        parcel.writeString(password);
        parcel.writeInt(id);
    }



    //getters only

    public String getDatabase() { return database; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    public int getId() { return id; }


}
