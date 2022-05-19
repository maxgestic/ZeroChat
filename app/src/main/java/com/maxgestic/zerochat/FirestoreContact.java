package com.maxgestic.zerochat;

import com.google.firebase.Timestamp;

import java.util.Comparator;

public class FirestoreContact implements Comparable<FirestoreContact>{
    public static final String FIELD_NICKNAME = "nickname";
    public static final String PATH = "users";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_ID = "id";
    public static final String FIELD_LASTMESSAGE = "lastMessage";

    private String nickname;
    private String id;
    private String email;
    private Timestamp lastMessage;
    private String messagePrev;
    private byte[] imageBytes;

    public FirestoreContact(){}

    public String getNick(){return nickname;}
    public String getID(){return id;}
    public String getEmail(){return email;}
    public Timestamp getLastMessage(){return lastMessage;}
    public String getMessagePrev() { return messagePrev; }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setNick (String nick){this.nickname = nick;}
    public void setID(String id){this.id = id;}
    public void setEmail(String email){this.email = email;}
    public void setLastMessage(Timestamp lastMessage){this.lastMessage = lastMessage;}
    public void setMessagePrev(String messagePrev) { this.messagePrev = messagePrev; }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    @Override
    public String toString(){
        return String.format("id: %s; nickname %s", id, nickname);
    }

    public static Comparator<FirestoreContact> byDate = new Comparator<FirestoreContact>() {
        @Override
        public int compare(FirestoreContact o1, FirestoreContact o2) {
            Timestamp t1, t2;
            t1 = o1.getLastMessage();
            t2 = o2.getLastMessage();
            return t2.compareTo(t1);

        }
    };

    @Override
    public int compareTo(FirestoreContact o) {
        return this.lastMessage.compareTo(o.lastMessage);
    }
}
