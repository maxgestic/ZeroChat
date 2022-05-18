package com.maxgestic.zerochat;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.sql.Time;
import java.util.Comparator;
import java.util.Date;

public class Message  implements Comparable<Message>{
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_READ = "read";
    public static final String FIELD_TIME_SENT = "time_sent";
    public static final String FIELD_SENT_BY = "sent_by";

    private String message;
    private Boolean read;
    private String sent_by;
    private Timestamp time_sent;



    public Message(String message, Boolean read, Timestamp time_sent, String sent_by){
        super();
        this.message = message;
        this.read = read;
        this.time_sent = time_sent;
        this.sent_by = sent_by;

    }

    public Message() {
    }

    public String getMessage(){return message;}
    public Boolean getRead(){return read;}
    public Timestamp getTimestamp(){return time_sent;}

    public String getSentBy() { return sent_by;}

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.time_sent = timestamp;
    }

    public void setSentBy(String sent_by) { this.sent_by = sent_by; }

    @NonNull
    @Override
    public String toString(){
        return String.format("message: %s; timestamp %s; read: %s; sentBy: %s", message, time_sent, read, sent_by);
    }

    public static Comparator<Message> byDate = new Comparator<Message>() {
        @Override
        public int compare(Message o1, Message o2) {
            Timestamp t1, t2;
            t1 = o1.getTimestamp();
            t2 = o2.getTimestamp();
            return t1.compareTo(t2);

        }
    };

    @Override
    public int compareTo(Message other) {
        return this.time_sent.compareTo(other.time_sent);
    }
}
