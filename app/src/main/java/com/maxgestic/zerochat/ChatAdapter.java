package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class ChatAdapter extends ArrayAdapter<Message> {

    private final List<Message> chatMessageList = new ArrayList<>();


    public ChatAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    @Override
    public void clear() {
        chatMessageList.clear();
        super.clear();
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public Message getItem(int index) {
        return this.chatMessageList.get(index);
    }

    @Override
    public void add(Message object) {
        chatMessageList.add(object);
        chatMessageList.sort(Message.byDate);
        super.add(object);
        super.sort(Message.byDate);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String userID = FirebaseAuth.getInstance().getUid();
        Message messageObj = getItem(position);
        View row;
        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Put message on the right side of screen depending if it is sent or received
        assert userID != null;
        if (userID.equals(messageObj.getSentBy())) {
            row = inflater.inflate(R.layout.message_sent, parent, false);
        }else{
            row = inflater.inflate(R.layout.message_received, parent, false);
        }
        //Get views and set message text and timestamp
        TextView messageTxt = (TextView) row.findViewById(R.id.msgr);
        TextView timestampTxt = row.findViewById(R.id.timestamp);
        messageTxt.setText(messageObj.getMessage());
        //calculate if the message was sent today or before, if sent today only show time, if yesterday or before show time and date
        Calendar cal = Calendar.getInstance();
        cal.setTime(messageObj.getTimestamp().toDate());
        Calendar c1 = Calendar.getInstance(); // today
        c1.add(Calendar.DAY_OF_YEAR, -1); // yesterday
        if (c1.get(Calendar.YEAR) >= cal.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) >= cal.get(Calendar.DAY_OF_YEAR)) {
            @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("dd.MM.yyyy").format(cal.getTime());
            timestampTxt.setText(format + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
        }else {
            timestampTxt.setText(cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
        }
        return row;
    }
}
